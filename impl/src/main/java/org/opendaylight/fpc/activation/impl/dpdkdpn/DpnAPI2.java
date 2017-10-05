/*
 * Copyright © 2016 - 2017 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.impl.dpdkdpn;


import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.List;

import org.json.JSONObject;
import org.opendaylight.fpc.impl.zeromq.ZMQSBListener;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.fpc.utils.IPToDecimal;
import org.opendaylight.fpc.utils.zeromq.ZMQClientSocket;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcagent.rev160803.ClientIdentifier;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.fpcbase.rev160803.FpcDpnId;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.IpAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DPDK DPN API over ZeroMQ.
 */
public class DpnAPI2 {
    private static byte CREATE_SESSION_TYPE = 0b0000_0001;
    private static byte MODIFY_DL_BEARER_TYPE = 0b0000_0010;
    private static byte DELETE_SESSION_TYPE = 0b0000_0011;
    private static byte MODIFY_UL_BEARER_TYPE = 0b0000_0100;
    private static byte CREATE_UL_BEARER_TYPE = 0b0000_0101;
    private static byte CREATE_DL_BEARER_TYPE = 0b0000_0110;
    private static byte DELETE_BEARER_TYPE = 0b0000_0110;
    private static byte HELLO = 0b0000_1000;
    private static byte BYE = 0b0000_1001;
    private static byte SEND_ADC_TYPE = 0b001_0001;
    private static byte DDN_ACK = 0b0000_0110;
	protected static final Logger LOG = LoggerFactory.getLogger(DpnAPI2.class);

    /**
     * Topic for broadcasting
     */
    public static byte BROADCAST_TOPIC = 0b0000_0000;

    ByteBuffer cs_bb = ByteBuffer.allocate(24);

    ZMQClientSocket sock;

    /**
     * Constructor
     * @param sock - ZeroMQ Socket
     */
    public DpnAPI2(ZMQClientSocket sock) {
        this.sock = sock;
    }

    /**
     * Creates Mobility Session
     * @param dpn - DPN
     * @param imsi - IMSI
     * @param ue_ip - Session IP Address
     * @param default_ebi - Default EBI
     * @param s1u_sgw_gtpu_ipv4 - SGW GTP-U IPv4 Address
     * @param s1u_sgw_gtpu_teid - SGW GTP-U TEID
     * @param clientIdentifier - Client Identifier
     * @param opIdentifier - Operation Identifier
     * @param sessionId - Session Id
     */
    public void create_session(
            Short dpn,
            BigInteger imsi,
            Ipv4Address ue_ip,
            Short default_ebi,
            Ipv4Address s1u_sgw_gtpu_ipv4,
            Long s1u_sgw_gtpu_teid,  // Although this is intended to be a Uint32
            Long clientIdentifier,
            BigInteger opIdentifier,
            Long sessionId
            //UlTftTable ul_tft_table
            )
    {
        create_session(dpn, imsi, IPToDecimal.ipv4ToLong(ue_ip.getValue()),
                default_ebi, s1u_sgw_gtpu_ipv4, s1u_sgw_gtpu_teid, clientIdentifier, opIdentifier, sessionId);
    }

    /**
     * Create Mobility Session.
     * @param dpn - DPN
     * @param imsi - IMSI
     * @param ue_ip - Session IPv4 Address
     * @param lbi - Linked Bearer Identifier
     * @param s1u_sgw_gtpu_ipv4 - SGW GTP-U IPv4 Address
     * @param s1u_sgw_gtpu_teid - SGW GTP-U TEID
     * @param clientIdentifier - Client Identifier
     * @param opIdentifier - Operation Identifier
     * @param sessionId - Session Id
     */
    public void create_session(
            Short dpn,
            BigInteger imsi,
            Long ue_ip,
            Short lbi,
            Ipv4Address s1u_sgw_gtpu_ipv4,
            Long s1u_sgw_gtpu_teid,  // Although this is intended to be a Uint32
            Long clientIdentifier,
            BigInteger opIdentifier,
            Long sessionId
            //UlTftTable ul_tft_table
            )
    {
        //Create byte[] from arguments
        ByteBuffer bb = ByteBuffer.allocate(41);
        bb.put(toUint8(dpn))
            .put(CREATE_SESSION_TYPE)
            .put(toUint64(imsi))
            .put(toUint8(lbi))
            .put(toUint32(ue_ip))
            .put(toUint32(s1u_sgw_gtpu_teid))
            .put(toUint32(IPToDecimal.ipv4ToLong(s1u_sgw_gtpu_ipv4.getValue())))
            .put(toUint64(BigInteger.valueOf(sessionId)))
            .put(toUint8(ZMQSBListener.getControllerTopic()))
            .put(toUint32(clientIdentifier))
            .put(toUint32(opIdentifier.longValue()));

        try {
            sock.getBlockingQueue().put(bb);
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        };
    }

    /**
     * Modify Downlink Bearer
     * @param dpn - DPN
     * @param s1u_sgw_gtpu_teid - SGW GTP-U TEID
     * @param s1u_enb_gtpu_ipv4 - ENodeB GTP-U IPv4 Address
     * @param s1u_enb_gtpu_teid - ENodeB GTP-U TEID
     * @param clientIdentifier - Client Identifier
     * @param opIdentifier - Operation Identifier
     */
    public void modify_bearer_dl(
            Short dpn,
            Long s1u_sgw_gtpu_teid,
            Ipv4Address s1u_enb_gtpu_ipv4,
            Long s1u_enb_gtpu_teid,
            Long clientIdentifier,
            BigInteger opIdentifier
    		)
    {
        ByteBuffer bb = ByteBuffer.allocate(23);
        bb.put(toUint8(dpn))
                .put(MODIFY_DL_BEARER_TYPE)
                .put(toUint32(IPToDecimal.ipv4ToLong(s1u_enb_gtpu_ipv4.getValue())))
                .put(toUint32(s1u_enb_gtpu_teid))
                .put(toUint32(s1u_sgw_gtpu_teid))
                .put(toUint32(clientIdentifier))
                .put(toUint32(opIdentifier.longValue()));
        try {
            sock.getBlockingQueue().put(bb);
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        };
    }

    /**
     * Delete Mobility Session.
     * @param dpn - DPN
     * @param del_default_ebi - Default EBI
     * @param s1u_sgw_gtpu_teid - SGW GTP-U TEID
     * @param clientIdentifier - Client Identifier
     * @param opIdentifier - Operation Identifier
     * @param sessionId - Session Id
     */
    public void delete_session(
            Short dpn,
            Short del_default_ebi,
            Long s1u_sgw_gtpu_teid,
            Long clientIdentifier,
            BigInteger opIdentifier,
            Long sessionId
    		)
    {
        ByteBuffer bb = ByteBuffer.allocate(19);
        bb.put(toUint8(dpn))
            .put(DELETE_SESSION_TYPE)
            .put(toUint64(BigInteger.valueOf(sessionId)))
            .put(toUint8(ZMQSBListener.getControllerTopic()))
            .put(toUint32(clientIdentifier))
            .put(toUint32(opIdentifier.longValue()));
        try {
            sock.getBlockingQueue().put(bb);
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        };
    }

    /**
     * Create Uplink Bearer.
     * @param dpn - DPN
     * @param imsi - IMSI
     * @param default_ebi - Default EBI
     * @param dedicated_ebi - Dedicated EBI
     * @param s1u_sgw_gtpu_ipv4 - SGW GTP-U IPv4 Address
     * @param s1u_sgw_gtpu_teid - SGW GTP-U TEID
     * @param ul_tft_table - Uplink TFT
     */
    public void create_bearer_ul(
            Short dpn,
            BigInteger imsi,
            Short default_ebi,
            Short dedicated_ebi,
            Ipv4Address s1u_sgw_gtpu_ipv4,
            Long s1u_sgw_gtpu_teid,
            Object ul_tft_table)
    {
        ByteBuffer bb = ByteBuffer.allocate(21);
        bb.put(toUint8(dpn))
            .put(CREATE_UL_BEARER_TYPE)
            .put(toUint64(imsi))
            .put(toUint8(default_ebi))
            .put(toUint8(dedicated_ebi))
            .put(toUint32(IPToDecimal.ipv4ToLong(s1u_sgw_gtpu_ipv4.getValue())))
            .put(toUint32(s1u_sgw_gtpu_teid));

        try {
            sock.getBlockingQueue().put(bb);
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        };
    }

    /**
     * Create Downlink Bearer.
     * @param dpn - DPN
     * @param dedicated_ebi - Default EBI
     * @param s1u_sgw_gtpu_teid - SGW GTP-U TEID
     * @param s1u_enb_gtpu_ipv4 - ENodeB GTP-U IPv4 Address
     * @param s1u_enb_gtpu_teid - ENodeB GTP-U TEID
     */
    public void create_bearer_dl(
            Short dpn,
            Short  dedicated_ebi,
            Long s1u_sgw_gtpu_teid,
            Ipv4Address s1u_enb_gtpu_ipv4,
            Long s1u_enb_gtpu_teid)
            //DlTft dl_tft_table)
    {
        ByteBuffer bb = ByteBuffer.allocate(16);
        bb.put(toUint8(dpn))
            .put(CREATE_DL_BEARER_TYPE)
            .put(toUint8(dedicated_ebi))
            .put(toUint32(s1u_sgw_gtpu_teid))
            .put(toUint32(IPToDecimal.ipv4ToLong(s1u_enb_gtpu_ipv4.getValue())))
            .put(toUint32(s1u_enb_gtpu_teid));

        try {
            sock.getBlockingQueue().put(bb);
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        };
    }

    /**
     * Modify Downlink Bearer.
     * @param dpn - DPN
     * @param s1u_sgw_gtpu_ipv4 - SGW GTP-U IPv4 Address
     * @param s1u_enb_gtpu_teid - ENodeB TEID
     * @param s1u_enb_gtpu_ipv4 - ENodeB GTP-U IPv4 Address
     * @param dl_tft_table - Downlink TFT
     * @param clientIdentifier - Client Identifier
     * @param opIdentifier - Operation Identifier
     * @param sessionId - Session Id
     */
    public void modify_bearer_dl(
            Short dpn,
            Ipv4Address s1u_enb_gtpu_ipv4,
            Long s1u_enb_gtpu_teid,
            Ipv4Address s1u_sgw_gtpu_ipv4,
            Object dl_tft_table,
            Long clientIdentifier,
            BigInteger opIdentifier,
            Long sessionId)
    {
        ByteBuffer bb = ByteBuffer.allocate(32);
        bb.put(toUint8(dpn))
            .put(MODIFY_DL_BEARER_TYPE)
            .put(toUint32(IPToDecimal.ipv4ToLong(s1u_sgw_gtpu_ipv4.getValue())))
            .put(toUint32(s1u_enb_gtpu_teid))
            .put(toUint32(IPToDecimal.ipv4ToLong(s1u_enb_gtpu_ipv4.getValue())))
            .put(toUint64(BigInteger.valueOf(sessionId)))
            .put(toUint8(ZMQSBListener.getControllerTopic()))
            .put(toUint32(clientIdentifier))
            .put(toUint32(opIdentifier.longValue()));

        try {
            sock.getBlockingQueue().put(bb);
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        };
    }

    /**
     * Modify Uplink Bearer.
     * @param dpn - DPN
     * @param s1u_enb_gtpu_ipv4 - ENodeB GTP-U IPv4 Address
     * @param s1u_enb_gtpu_teid - ENodeB GTP-U TEID
     * @param s1u_sgw_gtpu_teid - SGW GTP-U TEID
     * @param dl_tft_table - Downlink TFT
     */
    public void modify_bearer_ul(
            Short dpn,
            Ipv4Address s1u_enb_gtpu_ipv4,
            Long s1u_enb_gtpu_teid,
            Long s1u_sgw_gtpu_teid,
            Object dl_tft_table)
    {
        ByteBuffer bb = ByteBuffer.allocate(15);
        bb.put(toUint8(dpn))
            .put(MODIFY_UL_BEARER_TYPE)
            .put(toUint32(IPToDecimal.ipv4ToLong(s1u_enb_gtpu_ipv4.getValue())))
            .put(toUint32(s1u_enb_gtpu_teid))
            .put(toUint32(s1u_sgw_gtpu_teid));

        try {
            sock.getBlockingQueue().put(bb);
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        };
    }

    /**
     * Parses the JSON returned from the Control Plane and sends the ACK to the DPN
     * @param body - The JSON body returned by the Control Plane in the DDN ACK
     */
    public void ddnAck(JSONObject body){
    	ByteBuffer bb = ByteBuffer.allocate(14);
    	Short dpn = DpnAPIListener.getTopicFromDpnId(new FpcDpnId((String) body.get("dpn-id")));
        bb.put(toUint8(dpn))
        	.put(DDN_ACK);
        if(body.has("dl-buffering-duration"))
        	bb.put(toUint8((short) body.getInt("dl-buffering-duration")));
        if(body.has("dl-buffering-suggested-count"))
        	bb.put(toUint16((int) body.getInt("dl-buffering-suggested-count")));
        bb.put(toUint8(ZMQSBListener.getControllerTopic()))
        		.put(toUint32(Long.parseLong(new ClientIdentifier(body.getString("client-id")).getString())))
        		.put(toUint32(body.getLong("op-id")));
        try {
            sock.getBlockingQueue().put(bb);
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        };
    }

    /**
     * Hello Message
     * @param dpn - DPN
     * @param controllerId - Controller Id
     */
    public void hello(
            Short dpn,
            String controllerId)
    {
        ByteBuffer bb = ByteBuffer.allocate(3);
        bb.put(toUint8(dpn))
            .put(HELLO)
            .put(controllerId.getBytes());

        try {
            sock.getBlockingQueue().put(bb);
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        };
    }

    /**
     * Goodbye Message.
     * @param dpn - DPN
     * @param controllerId - Controller Id
     */
    public void bye(
            Short dpn,
            String controllerId)
    {
        ByteBuffer bb = ByteBuffer.allocate(3);
        bb.put(toUint8(dpn))
            .put(BYE)
            .put(controllerId.getBytes());

        try {
            sock.getBlockingQueue().put(bb);
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        };
    }

    /**
     * Schedule to Delete Bearer.
     * @param api - DpnAPI2
     * @param dpnTopic - DPN
     * @param s1u_sgw_gtpu_teid - SGW GTP-U TEID
     * @param time - Time in Seconds to schedule
     */
    public void delete_bearer(
            DpnAPI2 api,
            Short dpnTopic,
            Long s1u_sgw_gtpu_teid,
            Long time)
    {


        DeleteContextScheduler.getInstance().delete(api, dpnTopic, s1u_sgw_gtpu_teid, time);
    }

    /**
     * Delete Bearer.
     * @param dpnTopic - DPN
     * @param s1u_sgw_gtpu_teid - SGW GTP-U TEID
     */
    public void delete_bearer(
            Short dpnTopic,
            Long s1u_sgw_gtpu_teid)
    {
        ByteBuffer bb = ByteBuffer.allocate(7);
        bb.put(toUint8(dpnTopic))
            .put(DELETE_BEARER_TYPE)
            .put(toUint32(s1u_sgw_gtpu_teid));

        try {
            sock.getBlockingQueue().put(bb);
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        };
    }
    /**
     * Short to Byte
     * @param value - Short
     * @return byte value
     */
    public static byte toUint8(Short value) {
        return value.byteValue();
    }

    /**
     * Short to byte array
     * @param value - Short
     * @return byte array
     */
    public static byte[] toUint16(Short value) {
        return new byte[]{(byte)(value>>>8),(byte)(value&0xFF)};
    }

    /**
     * Lower two bytes of an integer to byte array
     * @param value - integer value
     * @return byte array
     */
    public static byte[] toUint16(Integer value) {
        return new byte[]{(byte)(value>>>8),(byte)(value&0xFF)};
    }

    /**
     * Long to byte array.
     * @param value - long
     * @return byte array
     */
    public static byte[] toUint32(long value) {
        return new byte[]{(byte)(value>>>24),(byte)(value>>>16),(byte)(value>>>8),(byte)(value&0xFF)};
    }

    /**
     * BigInteger to byte array.
     * @param value - BigInteger
     * @return byte array
     */
    public static byte[] toUint64(BigInteger value) {
        return new byte[]{value.shiftRight(56).byteValue(),value.shiftRight(48).byteValue(),value.shiftRight(40).byteValue(),
                value.shiftRight(32).byteValue(),value.shiftRight(24).byteValue(),value.shiftRight(16).byteValue(),
                value.shiftRight(8).byteValue(),value.and(BigInteger.valueOf(0xFF)).byteValue()};
    }

    /**
     * Creates the byte buffer to send ADC rules over ZMQ
     * @param topic - DPN Topic
     * @param domain_name - domain
     * @param ip - ipaddress/ipprefix (i.e. 127.0.0.1/32)
     * @param drop - Drop if 1
     * @param rating_group - Rating Group
     * @param service_ID - Service ID
     * @param sponsor_ID - Sponsor ID
     */
    public void send_ADC_rules(Short topic,
    		String domain_name, String ip, 
    		Short drop, Long rating_group,
    		Long service_ID, String sponsor_ID)
    {
    	Ipv4Address ip_address = null;
    	Short ip_prefix = null;
    	if(ip!=null){
    		String[] ip_split = ip.split("/");
    		ip_address = new Ipv4Address(ip_split[0]);
    		ip_prefix = Short.parseShort(ip_split[1]);
    	}
    	Short selector_type = (short) (domain_name != null ? 0 : ip_prefix != null ? 2 : ip_address != null ? 1 : 255);
    	if(selector_type == 255){
    		LOG.warn("Domain/IP not found, failed to send rules");
    		return;
    	}
    	ByteBuffer bb = ByteBuffer.allocate(200);
    	bb.put(toUint8(topic))
    	.put(SEND_ADC_TYPE)
    	.put(toUint8(selector_type));
    	if(selector_type == 0) {
			bb.put(toUint8((short)domain_name.length()))
			.put(domain_name.getBytes());
    	}
    	if((selector_type == 1) || (selector_type == 2)){
    		Long ip_address_long = IPToDecimal.ipv4ToLong(ip_address.getValue());
    		bb.put(toUint32(ip_address_long));
    	}
    	if(selector_type == 2){
    		bb.put(toUint16(ip_prefix));
    	}
    	if(drop!=null)
    		bb.put(toUint8(drop));
    	if(rating_group!=null)
    		bb.put(toUint32(rating_group));
    	if(service_ID!=null)
    		bb.put(toUint32(service_ID));
    	if(sponsor_ID!=null && (short)sponsor_ID.length()>0){
    		bb.put(toUint8((short)sponsor_ID.length()))
    		.put(sponsor_ID.getBytes());
    	}
    	bb.put(toUint8(ZMQSBListener.getControllerTopic()));
    	try {
    		LOG.info("Sending rules...");
            sock.getBlockingQueue().put(bb);
          } catch (InterruptedException e) {
            	ErrorLog.logError(e.getStackTrace());
          };
    }
}