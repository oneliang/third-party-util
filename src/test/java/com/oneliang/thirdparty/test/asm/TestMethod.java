package com.oneliang.thirdparty.test.asm;

public class TestMethod {

    public void a() {
        System.out.println("a()");
        c();
        b();
        d();
    }

    public void b() {
        System.out.println("b()");
    }

    public void c() {
        System.out.println("c()");
    }

    public void d() {
        System.out.println("d()");
    }
}
