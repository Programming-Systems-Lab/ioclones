package hitoshiio_test;

public class MultTester {
    public static int c = 2; //if it's not final, it's not a constant, and it SHOULD be an input!
    
    public static void main(String[] args) {
        int a = 3;
        int b = 4;
        int[] xx = {1,2,3,4,5,6};
        
        mult1(a, b);
        mult2(a, b);
        mult3(a, b);
        mult4(a, b);
        mult5(a, b);
        mult6(a, b);
        mult7(a, b);
        mult8(a, b);
        mult9(a, b);
        mult10(a, b);
        mult11(a, b);
        mult12(a, b);
        mult13(a, b);
        mult14(a, b);
        mult15(a, b);
        mult16(a, b);
        mult17(a, b);
        mult18(a, b, 5);
        mult19(a, b);
        mult20(a, b);
        mult21(a, b);
        mult22(a, b);
        mult23(a, b);
        mult24(a, b);
        mult25(a, xx);

        return;
    }

    public static int mult1(int x, int y) {
        return x * y;
    }

    public static int mult2(int x, int y) {
        if(x > y){
            return 0;
        } 
        else {
            return c;
        }
    }

    public static int mult3(int x, int y) {
        int z = 5;
        if (x > y) {
            return z;
        }
        else if (x == y){
            return 1;
        }
        else {
            return 0;
        }
    }

    public static int mult4(int x, int y) {
        return x * y * c;
    }

    public static int mult5(int x, int y) {
        if (y > 0) {
            return 1;
        }
        return 0;
    }
    
    public static int mult6(int x, int y) {
        boolean c;
           
        c = x < 5;
        if (c) {
            return 0;
        }
        else {
            return 1;
        }
    }

    public static int mult7(int x, int y) {
        int m = 0;
        if (x > 1) {
            m++;
        }
        else if (y < 0) {
            m++;
        }
        return m;
    }
    
    public static int mult8(int x, int y) {
        int total = 0;
        int sign = 1;

        if (x < 0) {
            sign = -1;
            x *= -1;
        }

        int i;
        for (i = 0; i < x; i++) {
            total += y;
        }
        return total * sign;
    }

    public static int mult9(int x, int y) {
        if (x > 0) {
            return 1;
        }
        else if (y > 0) {
            return 6;
        }
        return 9;
    }

    public static int mult10(int x, int y) {
        if (6 > x && 5 < x && 7 > x){
            return 7;
        }
        else if (x < 9){
            if (x == 0) {
                if (y == 0) {
                    return 0;
                }
            }
        }
        return 3;
    }

    public static int mult11(int x, int y) {
        int z = 2;
        for (int i = 0; i < 100; i++) {
            if (i == y) {
                z++;
            }
        }
        return z;
    }

    public static int mult12(int x, int y) {
        int sum = 0;
        while (sum < y) {
            sum++;
        }
        return sum;
    }

    public static int mult13(int x, int y) {
        c = 8;
        if (c > x){
            return y;
        }
        else {
            return 3;
        }
    }

    public static int mult14(int x, int y) {
        double u = 8.01;
        if (u > x){
            u += y;
            return (int) u;
        }
        return 6;
    }

    public static int mult15(int x, int y) {
        int sum = 0;
        do {
            sum ++;
        } while (sum < x);
        return sum;
    }

    public static int mult16(int x, int y) {
        return x;
    }

    public static int mult17(int x, int y) {
        if (c < 0) {
            return x * y;
        }
        return 0;
    }

    public static int mult18(int x, int y, int z) {
        int ret = 0;
        if (c < 0) {
            if (x == 9) {
                return 5;
            }
            return 0;
        }
        else if (y == 4) {
            for (int u=0; u < z; u++){
                ret--;
            }
        }
        return ret;
    }

    public static int mult19(int x, int y) {
        for (int i = c; i < (y*8); i++) {
            if (i > (x*y)) {
                return 1;
            }
        }
        return 0;
    }

    public static int mult20(int x, int y){
	    return (x < c) ? 0 : 1;
    }

    public static int mult21(int x, int y) {
        return (x < c) ? 0 : (y*y);
    }

    // this one is tricky - y is overloaded in second line, so there is a store before load
    // thus, y is not input
    public static int mult22(int x, int y){
        y = 4;
        if(y < 6) {
            return y;
        }
        return 1;
    }

    // iaload
    public static int mult23(int x, int y){
        int[] xx = {1,2,3,4,5,6};
        return xx[x];
    }
    
    public static int mult24(int x, int y){
        int[] xx = {1,2,3,4,5,6};
        y = xx[x];
        if(y < 6) {
            return y;
        }
        return 1;
    }
    
    // iastore
    public static int mult25(int x, int[] xx){
        xx[0] = x;

        return xx[1];
    }
}
 
