package dummy;

class MyDummyClass {
    public void foo() {
        System.out.println("Hello World");
    }

    private void bar() {
        System.out.println("Bar method");

        try {
            System.out.println("Do something dangerours");
        } catch (Exception e) {
            System.out.println("Catch behaviour");
        }
    }
}