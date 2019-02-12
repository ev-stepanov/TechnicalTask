import Exceptions.CircularReference;
import com.sun.javafx.css.Combinator;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class Program {
    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchFieldException, IOException {
        new Program().run();

    }
    public void run() {

        try {
            Student student = new Student();

            Map<String, List<Integer>> map = new HashMap<>();
            List<Integer> list = new LinkedList<>();
            list.add(4);
            list.add(2);
            map.put("hahaha", list);

            List<Integer> l = new ArrayList<>();
            l.add(33);
            l.add(34);
            map.put("ahahaha", l);

            student.setMap(map);

            Connector connector = new Connector();
            byte[] bytes =  connector.serialize(student);

            Student Ivan = (Student)connector.deserialize(bytes);
            System.out.println();
        }
        catch (CircularReference cr) {
            System.out.println(cr.getMessage());
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }
}
