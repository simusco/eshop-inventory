package com.zhouq.eshop.inventory.service.impl;

import com.zhouq.eshop.inventory.request.ProductInventoryCacheRefreshRequest;
import com.zhouq.eshop.inventory.request.ProductInventoryDBUpdateRequest;
import com.zhouq.eshop.inventory.request.Request;
import com.zhouq.eshop.inventory.request.RequestQueue;
import com.zhouq.eshop.inventory.service.RequertAsyncProcessService;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 请求异步处理的service 实现
 *
 * @author zhouq
 * @email zhouqiao@gmail.com
 * @date 2017/12/5 23:48
 */
@Service
public class RequertAsyncProcessServiceImpl implements RequertAsyncProcessService {
    @Override
    public void process(Request request) {
        try {
            RequestQueue requestQueue = RequestQueue.getInstance();
            Map<Integer, Boolean> flagMap = requestQueue.getFlagMap();
            if (request instanceof ProductInventoryDBUpdateRequest){
                //如果是一个更新数据库的请求,那么久将那个productId 对应的表示设置为 true
                flagMap.put(request.getProductId(),true);
            }else if (request instanceof ProductInventoryCacheRefreshRequest){
                //如果是缓存刷新请求,那么就判断如果表示不为空 而且是true 就说明前面已经有一个次商品id  的数据库更新请求.
                Boolean flag = flagMap.get(request.getProductId());
                if (flag != null && flag){
                    flagMap.put(request.getProductId(),false);
                }

                //如果是刷新缓存,而且表示不为空,但是标识是false
                //说明前面已经有一个数据更新 + 一个缓存刷新请求了.
                if (flag != null && !flag){
                    //对应这种请求,就直接过滤,不需要再放到内存队列里面去
                    return;
                }

            }

            // 做请求的路由 ，根据每个请求的商品ID 路由到对呀的队列中去
            ArrayBlockingQueue<Request> queue = getRoutingQueue(request.getProductId());
            //将请求放入对应的 队列中 完成路由操作
            queue.put(request);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取路由到的内存队列
     * @param productId 商品ID
     * @return 内存队列
     */
    private ArrayBlockingQueue<Request> getRoutingQueue(Integer productId){

        RequestQueue requestQueue = RequestQueue.getInstance();

        //获取productId 的hash 值
        String key = String.valueOf(productId);
        int h ;
        int hash = (key == null) ? 0: (h = key.hashCode()) ^ (h >>> 16);

        //对hash 值进行取模，将hash 值路由到指定的 内存队列中去
        int index = (requestQueue.queueSize() - 1) & hash;

        return requestQueue.getQueue(index);
    }
}