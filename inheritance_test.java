
public class inheritance_test {
    public static class Test1 {
        private String data;

        Test1(String _d) { data = _d; }
        public String test1() { return data; }
    }

    public static void main(String[] args) {
        Test1 t1 = new Test1("test1");
    }
}
