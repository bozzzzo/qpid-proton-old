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
package org.apache.qpid.proton.engine.jni;

import java.util.EnumSet;
import java.util.Iterator;
import org.apache.qpid.proton.engine.Delivery;
import org.apache.qpid.proton.engine.EndpointError;
import org.apache.qpid.proton.engine.EndpointState;
import org.apache.qpid.proton.engine.Link;
import org.apache.qpid.proton.engine.Session;
import org.apache.qpid.proton.jni.Proton;
import org.apache.qpid.proton.jni.SWIGTYPE_p_pn_link_t;
import org.apache.qpid.proton.jni.SWIGTYPE_p_pn_session_t;
import org.apache.qpid.proton.jni.SWIGTYPE_p_pn_terminus_t;
import org.apache.qpid.proton.jni.pn_durability_t;
import org.apache.qpid.proton.jni.pn_expiry_policy_t;
import org.apache.qpid.proton.jni.pn_terminus_type_t;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.UnsignedByte;
import org.apache.qpid.proton.amqp.UnsignedInteger;
import org.apache.qpid.proton.amqp.messaging.TerminusDurability;
import org.apache.qpid.proton.amqp.messaging.TerminusExpiryPolicy;
import org.apache.qpid.proton.amqp.transaction.Coordinator;
import org.apache.qpid.proton.amqp.transport.ReceiverSettleMode;
import org.apache.qpid.proton.amqp.transport.SenderSettleMode;
import org.apache.qpid.proton.amqp.transport.Source;
import org.apache.qpid.proton.amqp.transport.Target;

abstract class JNILink implements Link
{
    private SWIGTYPE_p_pn_link_t _impl;
    private Object _context;
    private JNISession _session;

    public JNILink(SWIGTYPE_p_pn_link_t link_t)
    {
        _impl = link_t;
        Proton.pn_link_set_context(_impl, this);
        _session = JNISession.getSession(Proton.pn_link_session(_impl));
    }

    @Override
    public String getName()
    {
        return Proton.pn_link_name(_impl);
    }

    @Override
    public Delivery delivery(byte[] tag, int offset, int length)
    {
        byte[] dup = new byte[length];
        System.arraycopy(tag,offset,dup,0,length);
        return JNIDelivery.getDelivery(Proton.pn_delivery(_impl, dup));
    }

    @Override
    public Delivery delivery(byte[] tag)
    {
        return JNIDelivery.getDelivery(Proton.pn_delivery(_impl, tag));
    }

    @Override
    public Iterator<Delivery> unsettled()
    {

        return null;  //TODO
    }

    @Override
    public Delivery current()
    {
        return JNIDelivery.getDelivery(Proton.pn_link_current(_impl));
    }

    @Override
    public boolean advance()
    {
        return Proton.pn_link_advance(_impl);
    }

    @Override
    public Source getSource()
    {
        return convertSource(Proton.pn_link_source(_impl));
    }

    @Override
    public Target getTarget()
    {
        return convertTarget(Proton.pn_link_target(_impl));
    }

    @Override
    public void setSource(Source source)
    {
        SWIGTYPE_p_pn_terminus_t source_t = Proton.pn_link_source(_impl);

        org.apache.qpid.proton.amqp.messaging.Source s = (org.apache.qpid.proton.amqp.messaging.Source) source;
        Proton.pn_terminus_set_address(source_t,s.getAddress());
        Proton.pn_terminus_set_dynamic(source_t,s.getDynamic());
        setDurability(source_t, s.getDurable() == null ? null : s.getDurable().getValue());
        setExpiryPolicy(source_t, s.getExpiryPolicy() == null ? null : s.getExpiryPolicy().getPolicy());
        if(s.getTimeout() != null)
        {
            Proton.pn_terminus_set_timeout(source_t, s.getTimeout().longValue());
        }
        Proton.pn_terminus_set_type(source_t, pn_terminus_type_t.PN_SOURCE);

        //TODO - capabilities

    }

    private static void setDurability(SWIGTYPE_p_pn_terminus_t terminus, UnsignedInteger durable)
    {
        if(durable != null)
        {
            if(durable.equals(TerminusDurability.NONE))
            {
                Proton.pn_terminus_set_durability(terminus, pn_durability_t.PN_NONDURABLE);
            }
            else if(durable.equals(TerminusDurability.CONFIGURATION))
            {
                Proton.pn_terminus_set_durability(terminus, pn_durability_t.PN_CONFIGURATION);
            }
            else if(durable.equals(TerminusDurability.UNSETTLED_STATE))
            {
                Proton.pn_terminus_set_durability(terminus, pn_durability_t.PN_DELIVERIES);
            }
        }
    }

    private static void setExpiryPolicy(SWIGTYPE_p_pn_terminus_t terminus, Symbol policy)
    {

            if(TerminusExpiryPolicy.NEVER.equals(policy))
            {
                Proton.pn_terminus_set_expiry_policy(terminus, pn_expiry_policy_t.PN_NEVER);
            }
            else if(TerminusExpiryPolicy.CONNECTION_CLOSE.equals(policy))
            {
                Proton.pn_terminus_set_expiry_policy(terminus, pn_expiry_policy_t.PN_CONNECTION_CLOSE);
            }
            else if(TerminusExpiryPolicy.SESSION_END.equals(policy))
            {
                Proton.pn_terminus_set_expiry_policy(terminus, pn_expiry_policy_t.PN_SESSION_CLOSE);
            }
            else if(TerminusExpiryPolicy.LINK_DETACH.equals(policy))
            {
                Proton.pn_terminus_set_expiry_policy(terminus, pn_expiry_policy_t.PN_LINK_CLOSE);
            }

    }

    @Override
    public void setTarget(Target target)
    {
        SWIGTYPE_p_pn_terminus_t target_t = Proton.pn_link_target(_impl);

        if(target instanceof org.apache.qpid.proton.amqp.messaging.Target)
        {
            org.apache.qpid.proton.amqp.messaging.Target t = (org.apache.qpid.proton.amqp.messaging.Target) target;
            Proton.pn_terminus_set_address(target_t, t.getAddress());
            Proton.pn_terminus_set_dynamic(target_t, t.getDynamic());
            setDurability(target_t, t.getDurable() == null ? null : t.getDurable().getValue());
            setExpiryPolicy(target_t, t.getExpiryPolicy() == null ? null : t.getExpiryPolicy().getPolicy());
            if(t.getTimeout() != null)
            {
                Proton.pn_terminus_set_timeout(target_t, t.getTimeout().longValue());
            }
            Proton.pn_terminus_set_type(target_t, pn_terminus_type_t.PN_TARGET);
        }
        else if(target instanceof Coordinator)
        {
            //TODO
            Proton.pn_terminus_set_type(target_t, pn_terminus_type_t.PN_COORDINATOR);
        }

    }

    @Override
    public Source getRemoteSource()
    {
        return convertSource(Proton.pn_link_remote_source(_impl));
    }

    private Source convertSource(SWIGTYPE_p_pn_terminus_t source_t)
    {
        if(source_t != null)
        {
            org.apache.qpid.proton.amqp.messaging.Source s = new org.apache.qpid.proton.amqp.messaging.Source();

            s.setAddress(Proton.pn_terminus_get_address(source_t));
            s.setDynamic(Proton.pn_terminus_is_dynamic(source_t));
            s.setTimeout(UnsignedInteger.valueOf(Proton.pn_terminus_get_timeout(source_t)));
            s.setDurable(convertDurability(source_t));
            s.setExpiryPolicy(convertExpiryPolicy(source_t));



            return s;
        }
        else
        {
            return null;
        }//TODO
    }

    private static TerminusExpiryPolicy convertExpiryPolicy(SWIGTYPE_p_pn_terminus_t source_t)
    {
        pn_expiry_policy_t expiry = Proton.pn_terminus_get_expiry_policy(source_t);
        TerminusExpiryPolicy policy = null;
        if(pn_expiry_policy_t.PN_NEVER.equals(expiry))
        {
            policy = TerminusExpiryPolicy.NEVER;
        }
        else if(pn_expiry_policy_t.PN_CONNECTION_CLOSE.equals(expiry))
        {
            policy = TerminusExpiryPolicy.CONNECTION_CLOSE;
        }
        else if (pn_expiry_policy_t.PN_SESSION_CLOSE.equals(expiry))
        {
            policy = TerminusExpiryPolicy.SESSION_END;
        }
        else if (pn_expiry_policy_t.PN_LINK_CLOSE.equals(expiry))
        {
            policy = TerminusExpiryPolicy.LINK_DETACH;
        }
        return policy;
    }

    private static TerminusDurability convertDurability(SWIGTYPE_p_pn_terminus_t source_t)
    {
        pn_durability_t durability = Proton.pn_terminus_get_durability(source_t);
        if(pn_durability_t.PN_NONDURABLE.equals(durability))
        {
            return TerminusDurability.NONE;
        }
        else if(pn_durability_t.PN_CONFIGURATION.equals(durability))
        {
            return TerminusDurability.CONFIGURATION;
        }
        else if(pn_durability_t.PN_DELIVERIES.equals(durability))
        {
            return TerminusDurability.UNSETTLED_STATE;
        }

        return null;
    }

    @Override
    public Target getRemoteTarget()
    {
        return convertTarget(Proton.pn_link_remote_target(_impl));
    }

    private Target convertTarget(SWIGTYPE_p_pn_terminus_t target_t)
    {
        if(target_t != null)
        {
            pn_terminus_type_t pn_terminus_type = Proton.pn_terminus_get_type(target_t);
            if(pn_terminus_type_t.PN_TARGET.equals(pn_terminus_type) || pn_terminus_type_t.PN_UNSPECIFIED.equals(pn_terminus_type))
            {
                org.apache.qpid.proton.amqp.messaging.Target t = new org.apache.qpid.proton.amqp.messaging.Target();

                t.setAddress(Proton.pn_terminus_get_address(target_t));
                t.setDynamic(Proton.pn_terminus_is_dynamic(target_t));
                t.setTimeout(UnsignedInteger.valueOf(Proton.pn_terminus_get_timeout(target_t)));
                t.setDurable(convertDurability(target_t));
                t.setExpiryPolicy(convertExpiryPolicy(target_t));

                return t;
            }
            else if(pn_terminus_type_t.PN_COORDINATOR.equals(pn_terminus_type))
            {
                Coordinator c = new Coordinator();

                // TODO

                return c;
            }
        }

        return null;
        //TODO
    }

    @Override
    public Link next(EnumSet<EndpointState> local, EnumSet<EndpointState> remote)
    {
        return getLink(Proton.pn_link_next(_impl, StateConverter.getStateMask(local, remote)));
    }

    static JNILink getLink(SWIGTYPE_p_pn_link_t link_t)
    {
        if(link_t != null)
        {
            JNILink link = (JNILink) Proton.pn_link_get_context(link_t);
            if(link == null)
            {
                if(Proton.pn_link_is_receiver(link_t))
                {
                    link = new JNIReceiver(link_t);
                }
                else if(Proton.pn_link_is_sender(link_t))
                {
                    link = new JNISender(link_t);
                }

            }
            return link;
        }
        return null;
    }


    @Override
    public int getCredit()
    {
        return Proton.pn_link_credit(_impl);
    }

    @Override
    public int getQueued()
    {
        return Proton.pn_link_queued(_impl);
    }

    @Override
    public int getUnsettled()
    {
        return Proton.pn_link_unsettled(_impl);
    }

    @Override
    public Session getSession()
    {
        return _session;
    }

    @Override
    public SenderSettleMode getSenderSettleMode()
    {
        // TODO
        return null;
    }

    @Override
    public void setSenderSettleMode(SenderSettleMode senderSettleMode)
    {
        //TODO
    }

    @Override
    public SenderSettleMode getRemoteSenderSettleMode()
    {
        return null;  //TODO
    }

    @Override
    public void setRemoteSenderSettleMode(SenderSettleMode remoteSenderSettleMode)
    {
        //TODO
    }

    @Override
    public ReceiverSettleMode getReceiverSettleMode()
    {
        return null;  //TODO
    }

    @Override
    public void setReceiverSettleMode(ReceiverSettleMode receiverSettleMode)
    {
        //TODO
    }

    @Override
    public ReceiverSettleMode getRemoteReceiverSettleMode()
    {
        return null;  //TODO
    }

    @Override
    public EndpointState getLocalState()
    {
        return StateConverter.getLocalState(Proton.pn_link_state(_impl));        
    }

    @Override
    public EndpointState getRemoteState()
    {
        return StateConverter.getRemoteState(Proton.pn_link_state(_impl));                
    }

    @Override
    public EndpointError getLocalError()
    {
        return null;  //TODO
    }

    @Override
    public EndpointError getRemoteError()
    {
        return null;  //TODO
    }

    @Override
    public void free()
    {
        if(_impl != null)
        {
            Proton.pn_link_set_context(_impl, null);
            Proton.pn_link_free(_impl);
            _impl = null;
        }
       
    }

    @Override
    public void open()
    {
        Proton.pn_link_open(_impl);
    }

    @Override
    public void close()
    {
        Proton.pn_link_close(_impl);
    }

    @Override
    public void setContext(Object o)
    {
        _context = o;
    }

    @Override
    public Object getContext()
    {
        return _context;
    }

    SWIGTYPE_p_pn_link_t getImpl()
    {
        return _impl;
    }

    @Override
    protected void finalize() throws Throwable
    {
        free();
        super.finalize();
    }
}
