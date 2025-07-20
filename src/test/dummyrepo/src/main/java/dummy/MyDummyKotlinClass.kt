package dummy;

import java.lang.Exception
import java.utils.String

class MyDummyClass {

    init {
        println("Do nothing")
    }

    fun foo() {
        println("Hello World");
    }

    fun bar() {
        println("Bar method");

        try {
            println("Do something dangerours");
        } catch (e: Exception) {
            println("Catch behaviour");
        }
    }

    private fun myPrivFoo(myVar: String) {
        println("This is a private method")
    }
}