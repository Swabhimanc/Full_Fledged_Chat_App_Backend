//package com.connecto;
//
//import com.connecto.model.Friend;
//import com.connecto.model.User;
//
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.List;
//
//public class Rough {
//    public static void main(String[] args) {
//        User sender = new User();
//        User recipient = new User();
//
//        Friend senderF = new Friend();
//        senderF.setId("Friend1");
//
//        Friend recipientF = new Friend();
//        recipientF.setId("Friend2");
//
//        sender.setFriends(Arrays.asList(recipientF));
//        recipient.setFriends(Arrays.asList(senderF));
//
//        System.out.println(sender.getFriends().toArray().toString());
//        System.out.println(recipient.getFriends().toArray().toString());
//
//        recipient.getFriends().add(recipient.updateUserAsFriend(sender));
//        sender.getFriends().add(sender.updateUserAsFriend(recipient));
//
//        System.out.println(sender.getFriends());
//        System.out.println(recipient.getFriends());
//    }
//}
