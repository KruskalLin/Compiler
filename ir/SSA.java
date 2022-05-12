package ir;

import error.ErrorMessage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static ir.TACTerm.insertTerms;

/**
 * @GlobalVariables store should be inserted before calling and function return
 */

public class SSA {
    private SymbolTable symbolTable;
    private List<TACTerm> terms;
    private List<List<CFGBlock>> cfgForest;
    private List<List<CFGBlock>> cfgReverseForest;
    private List<CFGBlock> cfgBlocks;
    private List<CFGBlock> rootBlocks;
    private List<CFGBlock> exitBlocks;
    private Queue<CFGBlock> queue;

    public SSA(SymbolTable symbolTable, List<TACTerm> terms, List<CFGBlock> cfgBlocks, List<CFGBlock> rootBlocks, List<CFGBlock> exitBlocks) throws ErrorMessage {
        cfgForest = new ArrayList<>();
        cfgReverseForest = new ArrayList<>();
        this.symbolTable = symbolTable;
        this.terms = terms;
        this.cfgBlocks = cfgBlocks;
        this.rootBlocks = rootBlocks;
        this.exitBlocks = exitBlocks;
        computeForest();
        computeDOMTree();
        computeReverseDOMTree();
        computePhi();
    }

    private void reset() {
        for (CFGBlock cfgBlock : cfgBlocks) cfgBlock.setVisited(false);
    }

    private void computePhi() throws ErrorMessage {
        for (int i = 0; i < rootBlocks.size(); i++) {
            CFGBlock root = rootBlocks.get(i);
            List<String> candidates = new ArrayList<>();
            int globalSize;
            if (root.getCurrentScope() == null) {
                // root
                List<String> globals = symbolTable.getGlobalVariables();
                globalSize = globals.size();
                candidates.addAll(globals);
            } else {
                FunctionSymbol functionSymbol = symbolTable.lookupFunctionSymbol(root.getBlockIndex());
                List<String> globals = new ArrayList<>(symbolTable.getGlobalVariables());
                List<String> params = new ArrayList<>(functionSymbol.getParams());
                List<String> variables = functionSymbol.getVariables();
                globals.removeAll(params); // shadow
                globals.removeAll(variables); // shadow
                globalSize = globals.size();
                params.removeAll(variables); // shadow
                candidates.addAll(globals);
                candidates.addAll(params);
                candidates.addAll(variables);
            }

            reset();
            DFSInsertStore(root, candidates, globalSize);
            reset();
//            DFSInsertLoad(root, candidates, globalSize);
//            reset();


            root.setVisited(true);
            queue = new LinkedList<>();
            queue.add(root);
            while (queue.size() != 0) {
                CFGBlock block = queue.poll();

                // insert PHI
                for (CFGBlock cfgBlock: block.getCfgChildren()) {
                    TACTerm firstTerm = cfgBlock.getTerms().get(0);
                    int firstLineCount = firstTerm.getLineCount();
                    if (firstTerm.getOps().equals("PHI")) {
                        for (int j = 0; j < candidates.size(); j++) {
                            Variable dstVariable = new Variable(candidates.get(j), 0);
                            cfgBlock.getTerms().get(j).insertDst(dstVariable);
                            cfgBlock.getTerms().get(j).addPHISource(block);
                        }
                    } else {
                        List<TACTerm> phiTerms = new ArrayList<>();
                        for (int j = 0; j < candidates.size(); j++) {
                            phiTerms.add(new TACTerm(firstLineCount + j, "PHI", candidates.get(j), new ArrayList<>(), Type.VOID));
                        }
                        insertTerms(firstLineCount - 1, terms, phiTerms);
                        cfgBlock.getTerms().addAll(0, phiTerms);
                        for (int j = 0; j < candidates.size(); j++) {
                            Variable dstVariable = new Variable(candidates.get(j), 0);
                            cfgBlock.getTerms().get(j).insertDst(dstVariable);
                            cfgBlock.getTerms().get(j).addPHISource(block);
                        }
                    }
                }

                List<CFGBlock> blocks = block.getIdoms();
                for (CFGBlock cfgBlock : blocks) {
                    if (!cfgBlock.isVisited()) {
                        cfgBlock.setVisited(true);
                        queue.add(cfgBlock);
                    }
                }
            }

            reset();
            root.setVisited(true);
            queue = new LinkedList<>();
            queue.add(root);
            while (queue.size() != 0) {
                CFGBlock block = queue.poll();

                List<Integer> indices = new ArrayList<>();
                // initial phi indices
                int firstLineCount = block.getTerms().get(0).getLineCount();
                if (!block.equals(root)) {
                    for (int j = 0; j < candidates.size(); j++) {
                        indices.add(firstLineCount + j);
                    }
                } else {
                    for (int j = 0; j < candidates.size(); j++) {
                        indices.add(0);
                    }
                }

                List<TACTerm> blockTerms = block.getTerms();
                for (TACTerm term: blockTerms) {
                    if (term.getOps().equals("CALL")) {
                        if (term.getDsts().size() > 0) {
                            List<Variable> dsts = term.getDsts();
                            for (int j = 0; j < dsts.size(); j++) {
                                for (int k = 0; k < candidates.size(); k++) {
                                    if (candidates.get(k).equals(dsts.get(j).getName())) {
                                        term.setDstsIndex(j, indices.get(k));
                                    }
                                }
                            }
                        }

                    } else {
                        for (int j = 0; j < candidates.size(); j++) {
                            if (term.getSrc() != null && candidates.get(j).equals(term.getSrc().getName())) {
                                if (term.getOps().equals("LG")) {
                                    indices.set(j, term.getLineCount());
                                }
                                term.getSrc().setIndex(indices.get(j));
                            }
                            if (term.getDst() != null && candidates.get(j).equals(term.getDst().getName())) {
                                if (term.getOps().equals("MOVE")) {
                                    indices.set(j, term.getLineCount());
                                }
                                term.getDst().setIndex(indices.get(j));
                            }
                        }
                    }
                }

                for (CFGBlock cfgBlock: block.getCfgChildren()) {
                    for (int j = 0; j < candidates.size(); j++) {
                        TACTerm phiTerm = cfgBlock.getTerms().get(j);
                        for (int k = 0; k < phiTerm.getDsts().size(); k++) {
                            if (phiTerm.getPhiSources().get(k).equals(block)) {
                                phiTerm.getDsts().get(k).setIndex(indices.get(j));
                            }
                        }
                    }
                }

                List<CFGBlock> blocks = block.getIdoms();
                for (CFGBlock cfgBlock : blocks) {
                    if (!cfgBlock.isVisited()) {
                        cfgBlock.setVisited(true);
                        queue.add(cfgBlock);
                    }
                }
            }


        }
    }

    private void DFSInsertLoad(CFGBlock block, List<String> candidates, int globalSize) {
        block.setVisited(true);

        int index = 0;
        List<TACTerm> tacTerms = block.getTerms();
        while (index < tacTerms.size()) {
            if (tacTerms.get(index).getOps().equals("CALL")) {
                List<TACTerm> loadTerms = new ArrayList<>();
                for (int j = 0; j < globalSize; j++) {
                    loadTerms.add(new TACTerm(tacTerms.get(index).getLineCount() + j + 1, "LG", new Variable(candidates.get(j), 0), null, Type.VOID, candidates.get(j)));
                }
                insertTerms(tacTerms.get(index).getLineCount(), terms, loadTerms);
                tacTerms.addAll(index + 1, loadTerms);
                index = index + globalSize;
            }
            index++;
        }

        for (CFGBlock cfgBlock : block.getCfgChildren()) {
            if (!cfgBlock.isVisited()) {
                DFSInsertLoad(cfgBlock, candidates, globalSize);
            }
        }
    }

    private void DFSInsertStore(CFGBlock block, List<String> candidates, int globalSize) {
        block.setVisited(true);

        int index = 0;
        List<TACTerm> tacTerms = block.getTerms();
        while (index < tacTerms.size()) {
            if (tacTerms.get(index).getOps().equals("MOVE")) {
                for (int j = 0; j < globalSize; j++) {
                    if (candidates.get(j).equals(tacTerms.get(index).getDst().getName())) {
                        List<TACTerm> storeTerms = new ArrayList<>();
                        storeTerms.add(new TACTerm(tacTerms.get(index).getLineCount() + 1, "SG", new Variable(candidates.get(j), 0), null, Type.VOID, candidates.get(j)));
                        insertTerms(tacTerms.get(index).getLineCount(), terms, storeTerms);
                        tacTerms.addAll(index + 1, storeTerms);
                        index++;
                        break;
                    }
                }
            }
            index++;
        }

        for (CFGBlock cfgBlock : block.getCfgChildren()) {
            if (!cfgBlock.isVisited()) {
                DFSInsertStore(cfgBlock, candidates, globalSize);
            }
        }
    }

    private void computeForest() {
        reset();
        for (int i = 0; i < rootBlocks.size(); i++) {
            cfgForest.add(new ArrayList<>());
            DFSForest(rootBlocks.get(i), i);
        }
        reset();
        for (int i = 0; i < exitBlocks.size(); i++) {
            cfgReverseForest.add(new ArrayList<>());
            DFSReverseForest(exitBlocks.get(i), i);
        }
    }

    private void DFSForest(CFGBlock block, int id) {
        block.setVisited(true);
        cfgForest.get(id).add(block);
        for (CFGBlock cfgBlock : block.getCfgChildren()) {
            if (!cfgBlock.isVisited()) {
                DFSForest(cfgBlock, id);
            }
        }
    }

    private void DFSReverseForest(CFGBlock block, int id) {
        block.setVisited(true);
        cfgReverseForest.get(id).add(block);
        for (CFGBlock cfgBlock : block.getCfgParents()) {
            if (!cfgBlock.isVisited()) {
                DFSReverseForest(cfgBlock, id);
            }
        }
    }


    private void computeDOMTree() {
        for (int i = 0; i < rootBlocks.size(); i++) {
            reset();
            CFGBlock root = cfgForest.get(i).get(0);
            root.getDoms().addAll(cfgForest.get(i));
            root.getDoms().remove(root);
            for (int j = 1; j < cfgForest.get(i).size(); j++) {
                CFGBlock removeBlock = cfgForest.get(i).get(j);
                removeBlock.setVisited(true);
                DFSCheckConnectivity(root);
                for (int k = 1; k < cfgForest.get(i).size(); k++) {
                    CFGBlock cfgBlock = cfgForest.get(i).get(k);
                    if (!cfgBlock.isVisited()) {
                        removeBlock.insertDoms(cfgBlock);
                    }
                }
                reset();
            }
            DFSConstructIDOMTree(root, null);
            reset();
        }
    }

    private void DFSCheckConnectivity(CFGBlock block) {
        block.setVisited(true);
        for (CFGBlock cfgBlock : block.getCfgChildren()) {
            if (!cfgBlock.isVisited()) {
                DFSCheckConnectivity(cfgBlock);
            }
        }
    }

    private void DFSConstructIDOMTree(CFGBlock block, CFGBlock father) {
        block.setVisited(true);
        if (father != null)
            father.insertIDoms(block);
        for (CFGBlock cfgBlock : block.getDoms()) {
            if (!cfgBlock.isVisited()) {
                DFSConstructIDOMTree(cfgBlock, block);
            }
        }
    }

    private void computeReverseDOMTree() {
        for (int i = 0; i < exitBlocks.size(); i++) {
            reset();
            CFGBlock root = cfgReverseForest.get(i).get(0);
            root.getReverseDoms().addAll(cfgReverseForest.get(i));
            root.getReverseDoms().remove(root);
            for (int j = 1; j < cfgReverseForest.get(i).size(); j++) {
                CFGBlock removeBlock = cfgReverseForest.get(i).get(j);
                removeBlock.setVisited(true);
                DFSCheckReverseConnectivity(root);
                for (int k = 1; k < cfgReverseForest.get(i).size(); k++) {
                    CFGBlock cfgBlock = cfgReverseForest.get(i).get(k);
                    if (!cfgBlock.isVisited()) {
                        removeBlock.insertReverseDoms(cfgBlock);
                    }
                }
                reset();
            }
            DFSConstructReverseIDOMTree(root, null);
        }
    }

    private void DFSCheckReverseConnectivity(CFGBlock block) {
        block.setVisited(true);
        for (CFGBlock cfgBlock : block.getCfgParents()) {
            if (!cfgBlock.isVisited()) {
                DFSCheckReverseConnectivity(cfgBlock);
            }
        }
    }

    private void DFSConstructReverseIDOMTree(CFGBlock block, CFGBlock father) {
        block.setVisited(true);
        if (father != null)
            father.insertReverseIDoms(block);
        for (CFGBlock cfgBlock : block.getReverseDoms()) {
            if (!cfgBlock.isVisited()) {
                DFSConstructReverseIDOMTree(cfgBlock, block);
            }
        }
    }

    public List<List<CFGBlock>> getCfgForest() {
        return cfgForest;
    }

    public List<List<CFGBlock>> getCfgReverseForest() {
        return cfgReverseForest;
    }
}
