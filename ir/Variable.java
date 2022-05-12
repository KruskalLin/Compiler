package ir;

import java.util.Objects;

import static parser.EBNFUtil.isNumeric;

public class Variable {
    String name;
    Integer index;

    public Variable(String name, Integer index) {
        this.name = name;
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public Integer getIndex() {
        return index;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public boolean isNumber() {
        return isNumeric(this.name);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Variable)) return false;
        Variable variable = (Variable) o;
        return getName().equals(variable.getName()) && getIndex().equals(variable.getIndex());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getIndex());
    }

    @Override
    public String toString() {
        return name + (index > 0 ? "_" + index : "");
    }
}
