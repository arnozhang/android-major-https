/**
 * Android Jungle-Major-Https framework project.
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

package com.jungle.majorhttps.model.base;

import android.content.Context;
import android.support.annotation.StringRes;
import android.util.Log;
import com.jungle.majorhttps.manager.MajorHttpClient;
import com.jungle.majorhttps.manager.MajorProgressLoadManager;
import com.jungle.majorhttps.model.listener.BothProxyModelListener;
import com.jungle.majorhttps.model.listener.ModelErrorListener;
import com.jungle.majorhttps.model.listener.ModelListener;
import com.jungle.majorhttps.model.listener.ModelLoadLifeListener;
import com.jungle.majorhttps.model.listener.ModelSuccessListener;
import com.jungle.majorhttps.request.base.NetworkResp;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractModel<
        Impl extends AbstractModel,
        Req extends AbstractModel.Request,
        Data> {

    public static final int INVALID_SEQ_ID = -1;
    public static final String TAG = "AbstractModel";


    public static class Request {

        private int mSeqId;
        private String mUrl;
        private ModelMethod mRequestMethod = ModelMethod.GET;
        private Map<String, Object> mRequestParams = new HashMap<>();
        private Map<String, String> mRequestHeaders = new HashMap<>();
        private byte[] mBody;
        private boolean mFillExtraHeader;


        public Request seqId(int seqId) {
            mSeqId = seqId;
            return this;
        }

        public Request url(String url) {
            mUrl = url;
            return this;
        }

        public Request method(ModelMethod type) {
            mRequestMethod = type;
            return this;
        }

        public Request head(String key, String value) {
            mRequestHeaders.put(key, value);
            return this;
        }

        public Request param(String key, Object obj) {
            mRequestParams.put(key, obj);
            return this;
        }

        public Request paramCheckNull(String key, Object obj) {
            if (obj != null) {
                mRequestParams.put(key, obj);
            }

            return this;
        }

        public Request addParams(Map<String, Object> params) {
            mRequestParams.putAll(params);
            return this;
        }

        public Request fillExtraHeader(boolean fill) {
            mFillExtraHeader = fill;
            return this;
        }

        public Request body(byte[] body) {
            mBody = body;
            return this;
        }

        public int getSeqId() {
            return mSeqId;
        }

        public String getUrl() {
            return mUrl;
        }

        public ModelMethod getRequestMethod() {
            return mRequestMethod;
        }

        public Map<String, Object> getRequestParams() {
            return mRequestParams;
        }

        public Map<String, String> getRequestHeaders() {
            return mRequestHeaders;
        }

        public boolean isFillExtraHeader() {
            return mFillExtraHeader;
        }

        public byte[] getBody() {
            return mBody;
        }
    }


    protected Req mRequest;
    protected ModelErrorListener mErrorListener;
    protected ModelSuccessListener<Data> mSuccessListener;
    protected ModelLoadLifeListener<Impl> mLoadLifeListener;
    protected ModelRequestFiller<Req> mModelFiller;
    protected MajorHttpClient mHttpClient;


    public AbstractModel() {
        mRequest = createRequest();
    }

    protected Req createRequest() {
        return (Req) new Request();
    }

    @SuppressWarnings("unchecked")
    public Impl url(String url) {
        mRequest.url(url);
        return (Impl) this;
    }

    @SuppressWarnings("unchecked")
    public Impl method(ModelMethod type) {
        mRequest.method(type);
        return (Impl) this;
    }

    @SuppressWarnings("unchecked")
    public Impl head(String key, String value) {
        mRequest.head(key, value);
        return (Impl) this;
    }

    @SuppressWarnings("unchecked")
    public Impl param(String key, Object obj) {
        mRequest.param(key, obj);
        return (Impl) this;
    }

    @SuppressWarnings("unchecked")
    public Impl body(byte[] body) {
        mRequest.body(body);
        return (Impl) this;
    }

    @SuppressWarnings("unchecked")
    public Impl paramCheckNull(String key, Object obj) {
        mRequest.paramCheckNull(key, obj);
        return (Impl) this;
    }

    @SuppressWarnings("unchecked")
    public Impl addParams(Map<String, Object> params) {
        mRequest.addParams(params);
        return (Impl) this;
    }

    @SuppressWarnings("unchecked")
    public Impl filler(ModelRequestFiller<Req> filler) {
        mModelFiller = filler;
        return (Impl) this;
    }

    @SuppressWarnings("unchecked")
    public Impl listener(ModelListener<Data> listener) {
        mSuccessListener = listener;
        mErrorListener = listener;
        return (Impl) this;
    }

    @SuppressWarnings("unchecked")
    public Impl success(ModelSuccessListener<Data> listener) {
        mSuccessListener = listener;
        return (Impl) this;
    }

    @SuppressWarnings("unchecked")
    public Impl error(ModelErrorListener listener) {
        mErrorListener = listener;
        return (Impl) this;
    }

    @SuppressWarnings("unchecked")
    public Impl lifeListener(ModelLoadLifeListener<Impl> listener) {
        mLoadLifeListener = listener;
        return (Impl) this;
    }

    @SuppressWarnings("unchecked")
    public Impl fillExtraHeader(boolean fill) {
        mRequest.fillExtraHeader(fill);
        return (Impl) this;
    }

    @SuppressWarnings("unchecked")
    public Impl client(MajorHttpClient client) {
        mHttpClient = client;
        return (Impl) this;
    }

    @SuppressWarnings("unchecked")
    public int load() {
        if (mLoadLifeListener != null) {
            mLoadLifeListener.onBeforeLoad((Impl) this);
        }

        if (mModelFiller != null) {
            mModelFiller.fill(mRequest);
        }

        return loadInternal();
    }

    public abstract int loadInternal();

    public int load(ModelListener<Data> listener) {
        return listener(listener).load();
    }

    public int load(ModelSuccessListener<Data> listener) {
        return success(listener).load();
    }

    public int loadWithProgress(Context context) {
        return MajorProgressLoadManager.getInstance().load(context, this, null);
    }

    public int loadWithProgress(Context context, ModelListener<Data> listener) {
        listener(listener);
        return loadWithProgress(context);
    }

    public int loadWithProgress(Context context, ModelSuccessListener<Data> listener) {
        success(listener);
        return loadWithProgress(context);
    }

    public int loadWithProgress(Context context, String loadingText) {
        return MajorProgressLoadManager.getInstance().load(context, this, loadingText);
    }

    public int loadWithProgress(Context context, String loadingText, ModelListener<Data> listener) {
        listener(listener);
        return loadWithProgress(context, loadingText);
    }

    public int loadWithProgress(Context context, String loadingText, ModelSuccessListener<Data> listener) {
        success(listener);
        return loadWithProgress(context, loadingText);
    }

    public int loadWithProgress(Context context, @StringRes int loadingText) {
        if (context == null) {
            return load();
        }

        return loadWithProgress(context, context.getString(loadingText));
    }

    public int loadWithProgress(Context context, @StringRes int loadingText, ModelListener<Data> listener) {
        listener(listener);
        return loadWithProgress(context, loadingText);
    }

    public int loadWithProgress(Context context, @StringRes int loadingText, ModelSuccessListener<Data> listener) {
        success(listener);
        return loadWithProgress(context, loadingText);
    }

    public void cancel() {
        getHttpClient().cancelBizModel(mRequest.getSeqId());
    }

    public int getSeqId() {
        return mRequest.getSeqId();
    }

    public ModelSuccessListener<Data> getSuccessListener() {
        return mSuccessListener;
    }

    public ModelErrorListener getErrorListener() {
        return mErrorListener;
    }

    public ModelListener<Data> getListener() {
        return new BothProxyModelListener<Data>(mSuccessListener, mErrorListener);
    }

    protected void doSuccess(NetworkResp networkResp, Data response) {
        if (mSuccessListener != null) {
            mSuccessListener.onSuccess(networkResp, response);
        }
    }

    protected void doError(int errorCode, String message) {
        Log.e(TAG, String.format("doError! errorCode=%d, url=%s, message=[ %s ].",
                errorCode, mRequest.getUrl(), message));

        if (mErrorListener != null) {
            mErrorListener.onError(errorCode, message);
        }
    }

    protected MajorHttpClient getHttpClient() {
        return mHttpClient != null ? mHttpClient : MajorHttpClient.getDefault();
    }
}
