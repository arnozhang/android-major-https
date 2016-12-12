/**
 * Android Jungle-Major-Http framework project.
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

package com.jungle.majorhttp.model.text;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.jungle.majorhttp.network.CommonError;
import com.jungle.majorhttp.request.base.NetworkResp;

public class JsonObjectRequestModel
        extends AbstractTextRequestModel<JsonObjectRequestModel, JSONObject> {

    @Override
    public void onSuccess(int seqId, NetworkResp networkResp, String response) {
        JSONObject json;
        try {
            json = JSON.parseObject(response);
        } catch (Exception e) {
            e.printStackTrace();
            doError(CommonError.PARSE_JSON_OBJECT_FAILED, e.getMessage());
            return;
        }

        doSuccess(networkResp, json);
    }
}
