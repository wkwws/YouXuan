package com.example.secondhand_project;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

public class test {



    @Test
    public void test1(){

        record dd(String aa,List<String> bb){}

        dd ddd=new dd("王葆青",List.of("唐人","kk"));

        String aa = ddd.aa;
        List<String> bb = ddd.bb;
        System.out.println(aa);
        System.out.println(bb);

        int[] s;



    }
}
