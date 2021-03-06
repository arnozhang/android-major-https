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

package com.jungle.majorhttps.model.text;

import com.jungle.majorhttps.model.base.AbstractModel;
import com.jungle.majorhttps.model.listener.ModelRequestListener;

public abstract class AbstractTextRequestModel<Impl extends AbstractTextRequestModel, Data>
        extends AbstractModel<Impl, AbstractModel.Request, Data>
        implements ModelRequestListener<String> {

    @Override
    public int loadInternal() {
        return getHttpClient().loadTextModel(mRequest, this);
    }

    @Override
    public void onError(int seqId, int errorCode, String message) {
        doError(errorCode, message);
    }
}
