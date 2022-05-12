package ir;

import java.util.Objects;

public class LiveVariable extends Variable{

    public LiveVariable(String name, Integer index) {
        super(name, index);
    }

    public LiveVariable(Variable variable) {
        super(variable.getName(), variable.getIndex());
    }

    public LiveVariable(String name) {
        super(name, 0);
    }
}
