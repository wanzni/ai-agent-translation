package cn.net.susan.ai.translation.security;

import cn.net.susan.ai.translation.entity.User;

public class UserContext {
    private static final ThreadLocal<User> HOLDER = new ThreadLocal<>();

    public static void set(User user){
        HOLDER.set(user);
    }

    public static User get(){
        return HOLDER.get();
    }

    public static Long getUserId(){
        User u = HOLDER.get();
        return u == null ? null : u.getId();
    }

    public static void clear(){
        HOLDER.remove();
    }
}