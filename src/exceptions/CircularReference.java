package exceptions;

public class CircularReference extends Exception {
    public String getMessage() {
        return "Serialization is impossible! There are cyclical links!";
    }
}
