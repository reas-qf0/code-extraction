import java.util.ArrayList;

public class output {
    static ArrayList<Integer> a = new ArrayList<>();
    static ArrayList<Integer> b = new ArrayList<>();

    private static int searchInA(int x) {
        for (int i = 0; i < a.size(); i++) {
            if (a.get(i) == x) return i;
        }
        return -1;
    }
    private static int searchInB(int x) {
        for (int i = 0; i < b.size(); i++) {
            if (b.get(i) == x) return i;
        }
        return -1;
    }

    public static void main(String[] args) {
        a.add(1);
        b.add(2);
        a.add(3);
        b.add(4);
        a.add(5);
        assert searchInA(1) == 0;
        assert searchInB(1) == -1;
        assert searchInA(2) == -1;
        assert searchInB(4) == 1;
        assert searchInA(-1) == -1;
        assert searchInB(-1) == -1;
    }
}