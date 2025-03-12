package org.example;
import org.example.domain.User;
import org.example.domain.Match;
import org.example.domain.Ticket;

public class Main {
    public static void main(String[] args) {
        User user = new User(1L, "Paul", "nimeniNimic123");
        System.out.println(user.getId());
        System.out.println(user.getUsername());
    }
}