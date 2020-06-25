package cc.ifnot.libs.utils;

public class MyClass {

    public static void main(String[] args) {

//        https://stackoverflow.com/questions/214741/what-is-a-stackoverflowerror
//        new Object() {
//            {
//                try {
//                    getClass().newInstance();
//                } catch (InstantiationException | IllegalAccessException e) {
//                    e.printStackTrace();
//                }
//            }
//        };


        Lg.d("md5 of %s -> %s", "hello", MD5.toHexString(
                MD5.md5("hello".getBytes())));
        Lg.tag("LogUtils");
        Lg.level(Lg.MORE);

        Lg.i("%s - %s", "b", "hekko");

        Lg.v("%s - %s", "b", "hekko");
        Lg.d("%s - %s", "b", "hekko");
        Lg.w("%s - %s", "b", "hekko");
        Lg.e("%s - %s", "b", "hekko");
        Lg.i("%s", "b", new Exception("test"));

        Lg.e("test: %s", new Exception() instanceof Throwable);
    }
}
