import java.util.*;
import java.io.*;
import java.math.*;

public class Agent1 {
    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int width = in.nextInt();
        int height = in.nextInt();
        int startGold = in.nextInt();
        for (int i = 0; i < 6; i++) {
            int entityType = in.nextInt();
            int maxHp = in.nextInt();
            int reach = in.nextInt();
            int attack = in.nextInt();
            int step = in.nextInt();
            int goldCost = in.nextInt();
            int woodCost = in.nextInt();
        }
        while (true) {
            int N = in.nextInt();
            for (int i = 0; i < N; i++) {
                int x = in.nextInt();
                int y = in.nextInt();
                int owner = in.nextInt();
                int entityType = in.nextInt();
                int health = in.nextInt();
            }
            System.out.println("WAIT");
        }
    }
}
