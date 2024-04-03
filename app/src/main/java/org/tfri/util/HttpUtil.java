package org.tfri.util;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

public class HttpUtil {
        private static final String baseUrl = "http://192.168.5.60:30000/"; // 服务器地址
//    private static final String baseUrl = "http://127.0.0.1:4523/m1/4265001-0-default"; // 服务器地址

    public static final String send = baseUrl;
    public static final String chat = baseUrl + "chat";
    private static final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)//设置连接超时时间
            .readTimeout(10, TimeUnit.SECONDS)//设置读取超时时间
            .build();

    public static boolean checkConnectNetwork(Context context) {
        ConnectivityManager conn = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo net = conn.getActiveNetworkInfo();
        return net != null && net.isConnectedOrConnecting();
    }

    /*
     * GET
     * */
    public static void httpGet(String url, ArrayList<String> params, Callback callback) {
        // build url
        StringBuilder getUrl = new StringBuilder(url);
        for (String s : params) {
            getUrl.append(s).append('/');
        }
        String finalUrl = getUrl.toString();

        // build request
        Request request = new Request.Builder().get()
                .url(finalUrl)
                .build();

        // request
        okHttpClient.newCall(request).enqueue(callback);
    }

    /*
     * POST
     * */
    public static void httpPost(String url, HashMap<String, String> params, Callback callback) {
        // build url
        JSONObject jsonBody = new JSONObject(params);

        // build request
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(jsonBody.toString(), MediaType.parse("application/json")))
                .build();

        // request
        okHttpClient.newCall(request).enqueue(callback);
    }

    public static void httpPostForObject(String url, HashMap<String, Object> params, Callback callback) {
        // build url
        JSONObject jsonBody = new JSONObject(params);

        // build request
        Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(jsonBody.toString(), MediaType.parse("application/json")))
                .build();

        // request
        okHttpClient.newCall(request).enqueue(callback);
    }

    /*
     * GET
     * */
    public static void httpGetForObject(String url, HashMap<String, Object> params, Callback callback) {
        // 构建请求URL，并添加参数
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            urlBuilder.addQueryParameter(entry.getKey(), entry.getValue().toString());
        }

        String finalUrl = urlBuilder.build().toString();

        // 构建GET请求
        Request request = new Request.Builder()
                .url(finalUrl)
                .build();

        // request
        okHttpClient.newCall(request).enqueue(callback);
    }

    /*
     * PUT
     * */
    public static void httpPut(String url, HashMap<String, String> params, Callback callback) {
        // build url
        JSONObject jsonBody = new JSONObject(params);

        // build request
        Request request = new Request.Builder()
                .url(url)
                .put(RequestBody.create(jsonBody.toString(), MediaType.parse("application/json")))
                .build();

        // request
        okHttpClient.newCall(request).enqueue(callback);
    }

    /*
     * DELETE
     * */
    public static void httpDelete(String url, ArrayList<String> params, Callback callback) {
        // build url
        StringBuilder deleteUrl = new StringBuilder(url);
        for (String s : params) {
            deleteUrl.append(s).append('/');
        }
        String finalUrl = deleteUrl.toString();

        // build request
        Request request = new Request.Builder().delete()
                .url(finalUrl)
                .build();

        // request
        okHttpClient.newCall(request).enqueue(callback);
    }

    /*
     * 上传图片
     * */
    /*
     * POST
     * */
    public static void postImage(String filepath, Callback callback) {
        // build request
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file",
                        filepath,
                        RequestBody.create(
                                new File(filepath),
                                MediaType.parse("application/octet-stream")
                        ))
                .build();

        Request request = new Request.Builder()
                .url("")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "")
                .addHeader("Content-Type", "multipart/form-data")
                .method("POST", body)
                .build();

        // request
        okHttpClient.newCall(request).enqueue(callback);
    }

    /*
     * 上传文件
     * */
    /*
     * POST
     * */
    public static void postFile(File file, String fileType, Callback callback) {
        // build url
        String postFileUrl = "";
        String finalUrl = postFileUrl + fileType + "/";

        // build request
        RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("file", "file", RequestBody.create(file, MediaType.parse("multipart/form-data")))
                .build();

        Request request = new Request.Builder()
                .url(finalUrl)
                .post(requestBody)
                .build();

        // request
        okHttpClient.newCall(request).enqueue(callback);
    }

    /*
     * 得到文件
     * */
    /*
     * GET
     * */
    public static void getFile(String fileName, String fileType, Context context, Callback callback) {
        // build url
        String getFileUrl = "";
        String finalUrl = getFileUrl + fileName + "/" + fileType + "/";
        fileName += ".";
        fileName += fileType;

        // build request
        Request request = new Request.Builder()
                .url(finalUrl)
                .build();

        // request
        okHttpClient.newCall(request).enqueue(callback);
    }
}
