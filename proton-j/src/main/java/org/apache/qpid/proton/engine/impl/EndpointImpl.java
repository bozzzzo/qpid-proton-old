/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */

package org.apache.qpid.proton.engine.impl;

import org.apache.qpid.proton.engine.Endpoint;
import org.apache.qpid.proton.engine.EndpointError;
import org.apache.qpid.proton.engine.EndpointState;

public abstract class EndpointImpl implements Endpoint
{
    private EndpointState _localState = EndpointState.UNINITIALIZED;
    private EndpointState _remoteState = EndpointState.UNINITIALIZED;
    private EndpointError _localError;
    private EndpointError _remoteError;
    private boolean _modified;
    private EndpointImpl _transportNext;
    private EndpointImpl _transportPrev;
    private Object _context;

    public void open()
    {
        switch(_localState)
        {
            case ACTIVE:
                // TODO
            case CLOSED:
                // TODO
            case UNINITIALIZED:
                _localState = EndpointState.ACTIVE;
        }
    }

    public void close()
    {

        switch(_localState)
        {
            case UNINITIALIZED:
                // TODO
            case CLOSED:
                // TODO
            case ACTIVE:
                _localState = EndpointState.CLOSED;
        }
    }

    public EndpointState getLocalState()
    {
        return _localState;
    }

    public EndpointState getRemoteState()
    {
        return _remoteState;
    }

    public EndpointError getLocalError()
    {
        return _localError;
    }

    public EndpointError getRemoteError()
    {
        return _remoteError;
    }

    void setLocalState(EndpointState localState)
    {
        _localState = localState;
    }

    void setRemoteState(EndpointState remoteState)
    {
        // TODO - check state change legal
        _remoteState = remoteState;
    }

    void setLocalError(EndpointError localError)
    {
        _localError = localError;
    }

    void setRemoteError(EndpointError remoteError)
    {
        _remoteError = remoteError;
    }

    void modified()
    {
        if(!_modified)
        {
            _modified = true;
            getConnectionImpl().addModified(this);
        }
    }

    protected abstract ConnectionImpl getConnectionImpl();

    void clearModified()
    {
        if(_modified)
        {
            _modified = false;
            getConnectionImpl().removeModified(this);
        }
    }

    boolean isModified()
    {
        return _modified;
    }

    EndpointImpl transportNext()
    {
        return _transportNext;
    }

    EndpointImpl transportPrev()
    {
        return _transportPrev;
    }

    public void free()
    {
        if(_transportNext != null)
        {
            _transportNext.setTransportPrev(_transportPrev);
        }
        if(_transportPrev != null)
        {
            _transportPrev.setTransportNext(_transportNext);
        }
    }

    void setTransportNext(EndpointImpl transportNext)
    {
        _transportNext = transportNext;
    }

    void setTransportPrev(EndpointImpl transportPrevious)
    {
        _transportPrev = transportPrevious;
    }

    public Object getContext()
    {
        return _context;
    }

    public void setContext(Object context)
    {
        _context = context;
    }
}
