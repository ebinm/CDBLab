package de.tum.i13.hashing;

public class Test {

    public Test() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println("Closing thread per connection kv server");
                shutDownMethod();
            }
        });
    }

    private void shutDownMethod() {
        System.out.println("invokated shutDownMethod succcessfully");
    }
}
