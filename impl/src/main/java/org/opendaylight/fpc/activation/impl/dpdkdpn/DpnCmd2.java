/*
 * Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.fpc.activation.impl.dpdkdpn;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.opendaylight.fpc.utils.ErrorLog;
import org.opendaylight.fpc.utils.zeromq.ZMQClientSocket;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Ipv4Address;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 * DPN API Command Line Test Harness
 */
public class DpnCmd2 {
    private static final String S1U_ENB_GTPU_IPV4 = "s1u_enb_gtpu_ipv4";
    private static final String S1U_ENB_GTPU_TEID = "s1u_enb_gtpu_teid";
    private static final String DEDICATED_EBI = "dedicated_ebi";
    private static final String DPN_ID = "1";
    private static final String S1U_SGW_GTPU_TEID = "s1u_sgw_gtpu_teid";
    private static final String S1U_SGW_GTPU_IPV4 = "s1u_sgw_gtpu_ipv4";
    private static final String DEFAULT_EBI = "default_ebi";
    private static final String UE_IP = "ue_ip";
    private static final String IMSI = "imsi";

    /**
     * Returns a Command Line Option
     * @param cmd - Command Line
     * @param option - Options
     * @return Option Value (as String)
     */
    private static String GetCmdOption(CommandLine cmd, String option) {
        if (cmd.hasOption(option)) {

           return cmd.getOptionValue(option);
        } else {
            System.err.println(option + " cannot be empty");
            System.exit(1);
            return null;
        }
    }

    /**
     * Test
     * @param args - Arguments
     */
    public static void main(String[] args) {

        // create Options object
        Options options = new Options();

        // add option
        options.addOption(
                Option.builder("method").
                    hasArg().
                    desc("method name").required().
                    build());
        options.addOption(
                Option.builder("ip").
                    hasArg().
                    desc("zeroMQ ip").required().
                    build());
        options.addOption(
                Option.builder("port").
                    hasArg().
                    desc("zeroMQ port").required().
                    build());
        //options.addOption("h", true, "help");

        // method option
        options.addOption(IMSI, true, "IMSI");
        options.addOption(UE_IP,true, "UE_IP");
        options.addOption(DEFAULT_EBI,true, DEFAULT_EBI);
        options.addOption(DEDICATED_EBI,true, DEDICATED_EBI);
        options.addOption(S1U_SGW_GTPU_IPV4, true, S1U_SGW_GTPU_IPV4);
        options.addOption(S1U_SGW_GTPU_TEID, true, S1U_SGW_GTPU_TEID);
        options.addOption("ul_tft_table", true, "ul_tft_table");
        options.addOption(S1U_ENB_GTPU_IPV4, true, S1U_ENB_GTPU_IPV4);
        options.addOption(S1U_ENB_GTPU_TEID, true, S1U_ENB_GTPU_TEID);


        HelpFormatter formatter = new HelpFormatter();
        //formatter.printHelp( "DpnAPICmd", options, true );

        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        try {
            cmd = parser.parse( options, args);
        } catch (ParseException e1) {
            // oops, something went wrong
            System.err.println( "Parsing failed.  Reason: " + e1.getMessage() );
        }

        String methodName = cmd.getOptionValue("method");
        cmd.getOptionValue("url");
        String ip = cmd.getOptionValue("ip");
        String port = cmd.getOptionValue("port");
        System.out.println("tcp://"+ip+":"+ port);
        CountDownLatch startSignal = new CountDownLatch(1);
        ZMQClientSocket socket = new ZMQClientSocket(new ZContext(), "tcp://"+ip+":"+ port, 1, startSignal,
                new LinkedBlockingQueue<ByteBuffer>());
        socket.open();
        Thread socketT = new Thread(socket);
        socketT.start();
        startSignal.countDown();
        try {
            Thread.currentThread().sleep(1000);
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        }
        DpnAPI2 dpnAPI = new DpnAPI2(socket);

        //create session
        if (methodName.compareTo("create_session") == 0){
            BigInteger imsi = new BigInteger(GetCmdOption(cmd, IMSI));
            Ipv4Address ue_ip = new Ipv4Address(GetCmdOption(cmd, UE_IP));
            Short default_ebi = Short.parseShort(GetCmdOption(cmd, DEFAULT_EBI));
            Ipv4Address s1u_sgw_gtpu_ipv4 = new Ipv4Address(GetCmdOption(cmd, S1U_SGW_GTPU_IPV4));
            Long s1u_sgw_gtpu_teid = Long.parseLong(GetCmdOption(cmd, S1U_SGW_GTPU_TEID));
            // UlTftTable ul_tft_table = null;

            System.out.println("Calling create_session");
            dpnAPI.create_session(DPN_ID, imsi, ue_ip, default_ebi, s1u_sgw_gtpu_ipv4, s1u_sgw_gtpu_teid);
            System.out.println("After calling create_session");

        } else if (methodName.compareTo("modify_bearer_ul") == 0) {
            Long s1u_sgw_gtpu_teid = Long.parseLong(GetCmdOption(cmd, S1U_SGW_GTPU_TEID));
            Ipv4Address s1u_enb_gtpu_ipv4 =  new Ipv4Address(GetCmdOption(cmd, S1U_ENB_GTPU_IPV4));
            Long s1u_enb_gtpu_teid = Long.parseLong(GetCmdOption(cmd, S1U_ENB_GTPU_TEID));

            System.out.println("Calling modify_bearer_ul");
            dpnAPI.modify_bearer_dl(DPN_ID, s1u_sgw_gtpu_teid, s1u_enb_gtpu_ipv4, s1u_enb_gtpu_teid);

        } else if (methodName.compareTo("create_bearer_ul") == 0) {
            BigInteger imsi = new BigInteger(GetCmdOption(cmd, IMSI));
            Short default_ebi = Short.parseShort(GetCmdOption(cmd, DEFAULT_EBI));
            Short dedicated_ebi = Short.parseShort(GetCmdOption(cmd, DEDICATED_EBI));
            Ipv4Address s1u_sgw_gtpu_ipv4 = new Ipv4Address(GetCmdOption(cmd, S1U_SGW_GTPU_IPV4));
            Long s1u_sgw_gtpu_teid = Long.parseLong(GetCmdOption(cmd, S1U_SGW_GTPU_TEID));

            System.out.println("Calling create_bearer_ul");
            dpnAPI.create_bearer_ul(DPN_ID, imsi, default_ebi, dedicated_ebi, s1u_sgw_gtpu_ipv4, s1u_sgw_gtpu_teid, null);
        }  else if (methodName.compareTo("create_bearer_dl") == 0) {
            Short dedicated_ebi = Short.parseShort(GetCmdOption(cmd, DEDICATED_EBI));
            Ipv4Address s1u_sgw_gtpu_ipv4 = new Ipv4Address(GetCmdOption(cmd, S1U_SGW_GTPU_IPV4));
            Long s1u_sgw_gtpu_teid = Long.parseLong(GetCmdOption(cmd, S1U_SGW_GTPU_TEID));
            Ipv4Address s1u_enb_gtpu_ipv4 =  new Ipv4Address(GetCmdOption(cmd, S1U_ENB_GTPU_IPV4));
            Long s1u_enb_gtpu_teid = Long.parseLong(GetCmdOption(cmd, S1U_ENB_GTPU_TEID));

            System.out.println("Calling create_bearer_dl");
            dpnAPI.create_bearer_dl(DPN_ID, dedicated_ebi, s1u_sgw_gtpu_teid, s1u_enb_gtpu_ipv4, s1u_enb_gtpu_teid);
        } else if (methodName.compareTo("modify_bearer_dl") == 0) {
            Long s1u_sgw_gtpu_teid = Long.parseLong(GetCmdOption(cmd, S1U_SGW_GTPU_TEID));
            Ipv4Address s1u_enb_gtpu_ipv4 =  new Ipv4Address(GetCmdOption(cmd, S1U_ENB_GTPU_IPV4));
            Long s1u_enb_gtpu_teid = Long.parseLong(GetCmdOption(cmd, S1U_ENB_GTPU_TEID));

            System.out.println("Calling modify_bearer_dl");
            dpnAPI.modify_bearer_dl(DPN_ID, s1u_sgw_gtpu_teid, s1u_enb_gtpu_ipv4, s1u_enb_gtpu_teid);
        } else if (methodName.compareTo("delete_bearer") == 0) {
            Long s1u_sgw_gtpu_teid = Long.parseLong(GetCmdOption(cmd, S1U_SGW_GTPU_TEID));

            System.out.println("Calling delete_bearer");
            dpnAPI.delete_bearer(DPN_ID, s1u_sgw_gtpu_teid);
        }

        else if (methodName.compareTo("delete_session") == 0) {
            Short default_ebi = Short.parseShort(GetCmdOption(cmd, DEFAULT_EBI));
            Long s1u_sgw_gtpu_teid = Long.parseLong(GetCmdOption(cmd, S1U_SGW_GTPU_TEID));

            System.out.println("Calling delete_session");
            dpnAPI.delete_session(DPN_ID, default_ebi, s1u_sgw_gtpu_teid);

        }
        try {
            Thread.currentThread().sleep(1000);
        } catch (InterruptedException e) {
        	ErrorLog.logError(e.getStackTrace());
        }
        socketT.interrupt();
        System.exit(0);
    }
}
