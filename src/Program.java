import exceptions.CircularReference;

import java.util.*;

public class Program {
    public static void main(String[] args){
        new Program().run();

    }
    private void run() {

        try {
            Student student = new Student();

            student.setFirstName("Ivan");
            student.setLastName("Ivanov");
            student.setAge(31);
            student.setCourse(2);
            student.setAverageRating(4.3);

            Map<String, List<Integer>> map = new HashMap<>();
            List<Integer> list = new LinkedList<>();
            list.add(4);
            list.add(2);
            map.put("one", list);

            List<Integer> l = new ArrayList<>();
            l.add(33);
            l.add(34);
            map.put("two", l);

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
