# README

## DLX Compiler Project

### Update

1. Finish most of the machine generation coding except the whole array assignment.
2. Pass all the tests except test018 which is the whole array assignment test.
3. Fix bugs of code address corruption and miscalculation of jumping line count, and also add empty block deletion and block type detection for easier optimization.
4.  Add instruction `SG` for global variable storing.



### Compiling Script for DOT Graph Generation

Since there are many assertions for type checking in this project, please add `-ea` argument in JVM for enabling assertions.

`TestIRGenerator.java` is for generating IR dot graphs. It supports 5 types of command lines. Each type should be like

```bash
javac -sourcepath . TestIRGenerator.java <code file> (<optimization>...)
```



#### SSA

For testing results before and after SSA, use a command line

```bash
javac -sourcepath . TestIRGenerator.java <code file> SSA
```



#### Optimization

For testing results before and after optimization, we define `AS` as arithmetic simplification, `CF` as constant folding, `CSE` as common subexpression elimination, `CP` as copy propagation, and constant propagation (this project does these two at the same time), and `DCE` as dead line elimination. Two types of methods are defined: `OneThrough` and `Repeat`. `OneThrough` means running the optimizations in order only one time, and the command line is like

```bash
javac -sourcepath . TestIRGenerator.java <code file> OneThrough <optimizations>
```

For example, we can use command line like this

```bash
javac -sourcepath . TestIRGenerator.java <code file> OneThrough AS CF CSE DCE CP
```



`Repeat` means running the optimizations in order until convergence, it may run in order several times. For example, we can use a command line like this

```bash
javac -sourcepath . TestIRGenerator.java <code file> Repeat AS CF CSE DCE CP
```



#### Register Allocation

For testing register allocation, we use numbers to define how many registers we use. The command line is

```bash
javac -sourcepath . TestIRGenerator.java <code file> <register number>
```



#### Other option

If we want to show the deleted line after DCE and also the store operations for global variables, we can add `ShowDeleted` after other arguments, like

```bash
javac -sourcepath . TestIRGenerator.java <code file> Repeat AS CF CSE DCE CP ShowDeleted
```



After running `TestIRGenerator.java`, pre-and post-dot graphs would be generated in `/output` with the same name as the input file.



### Script for Compiler

Run `TestCompiler` to test this project with DLX. Usage

```bash
javac -sourcepath . TestCompiler.java <code file> <data file> <register number> [ShowProgram]
```

The `ShowProgram` flag is to show the generated machine codes in the console. e.g.,

```
javac -sourcepath . TestCompiler.java ./code ./in 5 ShowProgram
```





### Several special designs in this project [included in the report but also here]

1. This project doesn’t propagate the optimizations through function calls. That means global variables would use index 0 in each function entry. (So this project doesn’t remove redundant load and store operations.)
2. Variable with index 0 would not be shown with index, e.g., only `a` is to be shown rather than `a_0`
3. `PHI` instructions are maintained in the optimization. This can be done by iteratively translating single argument `PHI` to `MOVE` operation, e.g. `3: PHI x_1` can be translated as `MOVE x_1 x_3`. And optimizations would propagate to `PHI` instructions.
4. This project does CP and CPP at the same time. Thus, the pre- and post-dot graphs of CP and CPP are shown together.
5. Local variables are pushed to the stack before entering the first line of the main or function.




### File Structure

`Compiler.java` is for Machine Code Generation.

`IRGenerator.java` together with Package `/ir` is for IR generation, IR optimization, and Register Allocation.

`Parser.java` and `Scanner.java` together with Package `/parser` and Package `/scanner` are for language parsing.

