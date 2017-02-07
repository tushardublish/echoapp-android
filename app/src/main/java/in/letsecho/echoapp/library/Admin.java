package in.letsecho.echoapp.library;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Tushar on 07-02-2017.
 */

public class Admin {
    private static List<String> ADMINS = Arrays.asList("vBnYd7839IMcCw0H5XaELsnMVfD2");

    public static boolean isAdmin(String userId) {
        return ADMINS.contains(userId);
    }
}
