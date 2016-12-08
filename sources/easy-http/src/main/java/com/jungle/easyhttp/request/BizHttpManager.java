/**
 * Android Jungle-Share framework project.
 *
 * Copyright 2016 Arno Zhang <zyfgood12@163.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.jungle.easyhttp.request;

import android.annotation.SuppressLint;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.RetryPolicy;
import com.android.volley.VolleyError;
import com.jungle.easyhttp.model.base.AbstractBizModel;
import com.jungle.easyhttp.model.binary.BinaryRequestModel;
import com.jungle.easyhttp.model.binary.DownloadFileRequestModel;
import com.jungle.easyhttp.model.binary.DownloadRequestModel;
import com.jungle.easyhttp.model.binary.UploadRequestModel;
import com.jungle.easyhttp.model.text.AbstractTextRequestModel;
import com.jungle.easyhttp.network.BaseRequestListener;
import com.jungle.easyhttp.network.CommonError;
import com.jungle.easyhttp.request.binary.BizBinaryRequest;
import com.jungle.easyhttp.request.binary.BizBinaryResponse;
import com.jungle.easyhttp.request.download.BizDownloadFileRequest;
import com.jungle.easyhttp.request.download.BizDownloadFileResponse;
import com.jungle.easyhttp.request.download.BizDownloadRequest;
import com.jungle.easyhttp.request.download.BizDownloadResponse;
import com.jungle.easyhttp.request.json.BizTextRequest;
import com.jungle.easyhttp.request.json.BizTextResponse;
import com.jungle.easyhttp.request.queue.RequestQueueFactory;
import com.jungle.easyhttp.request.upload.BizUploadRequest;
import com.jungle.easyhttp.request.upload.BizUploadResponse;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class BizHttpManager {

    private static final int DEFAULT_TIMEOUT_MS = 20 * 1000;
    private static final int UPLOAD_TIMEOUT_MS = 40 * 1000;


    private static BizHttpManager mInstance;

    public static BizHttpManager getInstance() {
        if (mInstance == null) {
            mInstance = new BizHttpManager();
            mInstance.onCreate();
        }

        return mInstance;
    }


    private static class RequestNode {
        int mSeqId;
        Type mResponseType;
        Request<?> mVolleyRequest;
        BaseRequestListener mListener;

        public RequestNode(
                int seqId, Request<?> request,
                Type responseType, BaseRequestListener listener) {

            mSeqId = seqId;
            mVolleyRequest = request;
            mResponseType = responseType;
            mListener = listener;
        }

        public RequestNode(int seqId, Request<?> request, BaseRequestListener listener) {
            this(seqId, request, null, listener);
        }
    }


    @SuppressLint("UseSparseArrays")
    private Map<Integer, RequestNode> mRequestList = new HashMap<>();
    private AtomicInteger mSeqIdGenerator = new AtomicInteger();
    private RetryPolicy mDefaultRetryPolicy;
    private RetryPolicy mUploadRetryPolicy;
    private RequestQueue mRequestQueue;
    private ExtraHeadersFiller mExtraHeadersFiller;


    public void onCreate() {
        setUploadTimeoutMilliseconds(UPLOAD_TIMEOUT_MS);
        setDefaultTimeoutMilliseconds(DEFAULT_TIMEOUT_MS);
    }

    public void onTerminate() {
        mRequestList.clear();
    }

    public void setRequestQueueFactory(RequestQueueFactory factory) {
        mRequestQueue = factory.createRequestQueue();
    }

    public void setUploadTimeoutMilliseconds(int milliseconds) {
        mUploadRetryPolicy = new DefaultRetryPolicy(milliseconds, 1, 1.0f);
    }

    public void setDefaultTimeoutMilliseconds(int milliseconds) {
        mDefaultRetryPolicy = new DefaultRetryPolicy(milliseconds, 1, 1.0f);
    }

    public void setExtraHeadersFiller(ExtraHeadersFiller filler) {
        mExtraHeadersFiller = filler;
    }

    public synchronized int loadTextModel(
            AbstractTextRequestModel.Request request, BaseRequestListener<String> listener) {

        int seqId = nextSeqId();
        request.seqId(seqId);
        BizTextRequest jsonRequest = new BizTextRequest(
                seqId, request.getRequestMethod().toVolleyMethod(),
                request.getUrl(), request.getRequestParams(),
                request.getRequestHeaders(), request.getRequestBody(),
                mBizTextRequestListener);

        addRequestNode(seqId, request, jsonRequest, listener);
        return seqId;
    }

    public synchronized int loadBinaryModel(
            BinaryRequestModel.Request request, BaseRequestListener<byte[]> listener) {

        int seqId = nextSeqId();
        request.seqId(seqId);
        BizBinaryRequest binaryRequest = new BizBinaryRequest(
                seqId, request.getUrl(), request.getRequestParams(),
                request.getRequestHeaders(), request.getBody(),
                mBizBinaryRequestListener);

        addRequestNode(seqId, request, binaryRequest, listener);
        return seqId;
    }

    public synchronized int loadUploadModel(
            UploadRequestModel.Request request, BaseRequestListener<String> listener) {

        int seqId = nextSeqId();
        request.seqId(seqId);
        BizUploadRequest uploadRequest = new BizUploadRequest(
                seqId, request.getUrl(), request.getFormItems(),
                request.getRequestHeaders(),
                mBizUploadRequestListener);

        addRequestNode(seqId, request, uploadRequest, listener);
        return seqId;
    }

    public synchronized int loadDownloadModel(
            DownloadRequestModel.Request request, BaseRequestListener<byte[]> listener) {

        int seqId = nextSeqId();
        request.seqId(seqId);
        BizDownloadRequest uploadRequest = new BizDownloadRequest(
                seqId, request.getUrl(), request.getRequestParams(),
                request.getRequestHeaders(),
                mBizDownloadRequestListener);

        addRequestNode(seqId, request, uploadRequest, listener);
        return seqId;
    }

    public synchronized int loadDownloadFileModel(
            DownloadFileRequestModel.Request request, BaseRequestListener<String> listener) {

        int seqId = nextSeqId();
        request.seqId(seqId);
        BizDownloadFileRequest uploadRequest = new BizDownloadFileRequest(
                seqId, request.getUrl(), request.getRequestParams(),
                request.getRequestHeaders(), request.getFilePath(),
                mBizDownloadFileRequestListener);

        addRequestNode(seqId, request, uploadRequest, listener);
        return seqId;
    }

    private void addRequestNode(
            int seqId,
            AbstractBizModel.Request modelRequest,
            BizBaseRequest<?> request,
            BaseRequestListener listener) {

        if (request instanceof BizUploadRequest) {
            request.setRetryPolicy(mUploadRetryPolicy);
        } else {
            request.setRetryPolicy(mDefaultRetryPolicy);
        }

        if (modelRequest.getFillExtraHeader()) {
            request.setExtraHeadersFiller(mExtraHeadersFiller);
        }

        mRequestList.put(seqId, new RequestNode(seqId, request, listener));
        mRequestQueue.add(request);
    }

    public synchronized void cancelBizModel(int seqId) {
        RequestNode node = mRequestList.remove(seqId);
        if (node != null) {
            node.mVolleyRequest.cancel();
        }
    }

    public RequestQueue getRequestQueue() {
        return mRequestQueue;
    }

    private int nextSeqId() {
        return mSeqIdGenerator.addAndGet(1);
    }

    private WrappedRequestListener<BizTextResponse> mBizTextRequestListener =
            new WrappedRequestListener<BizTextResponse>() {
                @Override
                protected void handleSuccess(int seqId,
                        BaseRequestListener<BizTextResponse> listener,
                        BizBaseResponse<BizTextResponse> response) {

                    if (response == null) {
                        listener.onSuccess(seqId, null, null);
                        return;
                    }

                    listener.onSuccess(seqId, response.mHeaders, response.mContent);
                }
            };

    private WrappedRequestListener<BizBinaryResponse> mBizBinaryRequestListener =
            new WrappedRequestListener<BizBinaryResponse>() {
                @Override
                protected void handleSuccess(int seqId,
                        BaseRequestListener<BizBinaryResponse> listener,
                        BizBaseResponse<BizBinaryResponse> response) {

                    if (response == null) {
                        listener.onSuccess(seqId, null, null);
                        return;
                    }

                    listener.onSuccess(seqId, response.mHeaders, response.mContent);
                }
            };

    private WrappedRequestListener<BizUploadResponse> mBizUploadRequestListener =
            new WrappedRequestListener<BizUploadResponse>() {
                @Override
                protected void handleSuccess(int seqId,
                        BaseRequestListener<BizUploadResponse> listener,
                        BizBaseResponse<BizUploadResponse> response) {

                    if (response == null) {
                        listener.onSuccess(seqId, null, null);
                        return;
                    }

                    listener.onSuccess(seqId, response.mHeaders, response.mContent);
                }
            };

    private WrappedRequestListener<BizDownloadResponse> mBizDownloadRequestListener =
            new WrappedRequestListener<BizDownloadResponse>() {
                @Override
                protected void handleSuccess(int seqId,
                        BaseRequestListener<BizDownloadResponse> listener,
                        BizBaseResponse<BizDownloadResponse> response) {

                    if (response == null) {
                        listener.onSuccess(seqId, null, null);
                        return;
                    }

                    listener.onSuccess(seqId, response.mHeaders, response.mContent);
                }
            };

    private WrappedRequestListener<BizDownloadFileResponse> mBizDownloadFileRequestListener =
            new WrappedRequestListener<BizDownloadFileResponse>() {
                @Override
                protected void handleSuccess(int seqId,
                        BaseRequestListener<BizDownloadFileResponse> listener,
                        BizBaseResponse<BizDownloadFileResponse> response) {

                    if (response == null) {
                        listener.onSuccess(seqId, null, null);
                        return;
                    }

                    listener.onSuccess(seqId, response.mHeaders, response.mContent);
                }
            };


    private abstract class WrappedRequestListener<T> implements BizRequestListener<T> {

        protected abstract void handleSuccess(
                int seqId, BaseRequestListener<T> listener, BizBaseResponse<T> response);


        @SuppressWarnings("unchecked")
        @Override
        public void onSuccess(int seqId, BizBaseResponse<T> response) {
            synchronized (BizHttpManager.this) {
                RequestNode node = mRequestList.remove(seqId);
                if (node == null || node.mListener == null) {
                    return;
                }

                BaseRequestListener<T> listener = (BaseRequestListener<T>) node.mListener;
                handleSuccess(seqId, listener, response);
            }
        }

        @Override
        public void onError(int seqId, VolleyError error) {
            handleError(seqId, error);
        }
    }

    private synchronized void handleError(int seqId, VolleyError error) {
        RequestNode node = mRequestList.remove(seqId);
        if (node == null || node.mListener == null) {
            return;
        }

        node.mListener.onError(seqId, CommonError.fromError(error), error.toString());
    }
}