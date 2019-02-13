import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class Student implements Serializable {
    private String firstName;
    private String lastName;
    private int age;
    private int course;
    private double averageRating;
    private Boolean isMarried;

    private  Map<String, List<Integer>> map;
    private List<Integer> list;

    public List<Integer> getList() {
        return list;
    }

    public void setList(List<Integer> list) {
        this.list = list;
    }

    public Map<String, List<Integer>> getMap() {
        return map;
    }

    public void setMap(Map<String, List<Integer>> map) {
        this.map = map;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getCourse() {
        return course;
    }

    public void setCourse(int course) {
        this.course = course;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }

    public Boolean getMarried() {
        return isMarried;
    }

    public void setMarried(Boolean married) {
        isMarried = married;
    }
}
