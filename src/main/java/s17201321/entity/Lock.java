package s17201321.entity;

public class Lock {
    private static Lock lock;
    private static Lock lock2 = new Lock();// 用来控制新peer的加入与在peer加入前其它已peer正在上传文件server正在进行文件分配过程的锁
    private static final Lock lock3 = new Lock();

    private Lock(){

    }

    public static Lock getLock(){
        if (null!=lock)
            return lock;
        else
            return new Lock();
    }

    public static Lock getLock2(){
        return lock2;
    }

    public static Lock getLock3(){
        return lock3;
    }
}
