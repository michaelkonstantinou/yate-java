package com.mkonst.types;

public class ClassMethod {

    private String className;
    private String methodName;
    public ClassMethod(String className, String methodName) {
        this.className = className;
        this.methodName = methodName;
    }

    public ClassMethod(String classMethodQualifiedName) throws Exception {
        String[] qualifiedNames = classMethodQualifiedName.split("#");
        if (qualifiedNames.length != 2) {
            throw new Exception("The invoked ClassMethod constructor requires a string of the format classQualifiedName#methodName");
        }

        this.className = qualifiedNames[0];
        this.methodName = qualifiedNames[1];
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String toString() {
        return this.className + "#" + this.methodName;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ClassMethod) {
            ClassMethod cmToCompare = (ClassMethod) obj;
            return cmToCompare.className.equals(this.className) && cmToCompare.methodName.equals(this.methodName);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return className.hashCode() + 31 * methodName.hashCode();
    }
}

