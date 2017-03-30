package net.andreinc.aleph;

import static net.andreinc.aleph.AlephFormatter.template;

/**
 * Created by andreinicolinciobanu on 30/03/17.
 */
public class MicroBenchmark {
    public static void main(String[] args) {
        bench2();
    }

    public static final void bench1() {
        long start = System.currentTimeMillis();
        String format1 = "%s a %s b %s c %s d %s";
        for(int i = 0; i < 1000000; i++) {
            String.format(format1, "a", "b", "c", "d", "e");
        }
        System.out.println(System.currentTimeMillis() - start);
    }

    public static final void bench2() {
        long start = System.currentTimeMillis();
        String format2 = "#{a} a #{b} b #{c} c #{d} d #{e}";
        for(int i = 0; i < 1000000; i++) {
            template(format2, "a", "a", "b", "b", "c", "c", "d", "d", "e", "e").fmt();
        }
        System.out.println(System.currentTimeMillis() - start);
    }
}
