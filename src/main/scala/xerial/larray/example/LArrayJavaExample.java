package xerial.larray.example;

import scala.runtime.AbstractFunction1;
import xerial.larray.*;
import xerial.larray.japi.LArrayJ;

import java.io.File;

/**
 * LArray example in Java
 * @author Taro L. Saito
 */
public class LArrayJavaExample {

    public static void main(String[] args) {
        // Create a new LArray of Int type
        LIntArray l = LArrayJ.newLIntArray(5);

        // Read elements
        int e0 = l.apply(0);
        int e1 = l.apply(1);

        // Set elements
        for(int i=0; i<l.size(); ++i)
            l.update(i, i);

        // print the elements
        System.out.println(l.mkString(", "));

        // Traverse the elements
        int index = 0;
        for(Object e : l.ji()) {
            System.out.println(String.format("l(%d) = %d", index, (Integer)e));
            index += 1;
        }

        // manipulate LArray
        LIterator<?> l2 = l.map(new AbstractFunction1<Object, Object>(){
            public Object apply(Object v1) {
                return Integer.class.cast(v1) * 10;
            }
        });
        LIterator<?> f = l.filter(new AbstractFunction1<Object, Object>(){
            public Object apply(Object v1) {
                return Integer.class.cast(v1) % 2 == 0;
            }
        });
        LArray s = l.slice(2);

        // Build LArray
        LIntArrayBuilder b = new LIntArrayBuilder();
        for(int i=0; i<10; i += 3)
            b.append(i);
        LIntArray lb = b.result(); // LArray(0, 3, 6, 9)
        System.out.println(lb.mkString(", "));

        // Save to a file
        File file = l.saveTo(new File("target/larray.tmp"));
        file.deleteOnExit();

        // Load from a file
        LArray l3 = LArrayJ.loadLIntArrayFrom(file); // LArray(0, 1, 2, 3, 4)

        // Initialize the array
        l.clear();
        System.out.println(l.mkString(", ")); // 0, 0, 0, 0, 0

        // Release the memory contents
        l.free();

        System.out.println("done.");
    }
}
