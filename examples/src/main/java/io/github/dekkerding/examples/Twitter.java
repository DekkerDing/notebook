package io.github.dekkerding.examples;

import java.util.*;
import java.util.stream.Collectors;

/**
 * JDK 1.8
 *  设计一个简化版的推特(Twitter)，可以让用户实现发送推文，关注/取消关注其他用户，能够看见关注人（包括自己）的最近 10 条推文
 */
public class Twitter {

    private Map followMap  =new LinkedHashMap<String ,List>();

    private Map postTweetMap  =new LinkedHashMap<String ,Stack<String>>();

    private Deque stack = new ArrayDeque<String>();

    public  String postTweet( String userId,String tweetId ){
        // 根据给定的tweetId 和userId 创建一条新推文。每次调用此函数都会使用一个不同的tweetId
        long l = System.currentTimeMillis();
        stack.push(l+":"+userId+":"+tweetId);
        postTweetMap.put(userId,stack);
        return tweetId;
    }

    public String follow(String followerId, String followeeId){
        // 关注其他用户
        // 查询 获取 followerId 列表
        List followeeList = (List) followMap.get(followerId);
        if(followeeList.parallelStream().anyMatch(f-> !Boolean.parseBoolean(followeeId)))
        followeeList.add(followeeId);
        followMap.put(followerId,followeeList);

        return "关注成功 / 失败";
    }

    public String unfollow(String followerId, String followeeId){
        // 取消关注其他用户
        // 查询 获取 followerId 列表
        List followeeList = (List) followMap.get(followerId);
        //校验 关注列表中是否存在该用户 如何有就进行 更新 覆盖 替换
        followeeList.parallelStream().anyMatch(f-> Boolean.parseBoolean(followeeId));
        List followeeList_new = new LinkedList<String>();
        followeeList_new.addAll((Collection) followeeList.stream().filter(f-> !followeeId.equals(f)).collect(Collectors.toList()));
        return "取消关注成功 / 失败";
    }



    public List<String> getNewsFeed(String userId){

        List list = new LinkedList<String>();

        // 获取关注列表
        List followeeList = (List) followMap.get(userId);
        followeeList.parallelStream().forEach(f->{
            // 获取推文
            Stack<String> tweetStack = (Stack<String>) postTweetMap.get(f);
            list.add(tweetStack);
        });
        // 出栈
        return list;
    }
}