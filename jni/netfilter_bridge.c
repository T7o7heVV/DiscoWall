#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <netinet/in.h>
#include <linux/types.h>
#include <linux/netfilter.h>		/* for NF_ACCEPT */
#include <errno.h>

#include <linux/netfilter_ipv4.h>
// #include <linux/tcp.h>
// #include <linux/ip.h>
#include <linux/in.h>

/* Package handling */
#include<string.h>    //memset
#include<netinet/ip_icmp.h>   //Provides declarations for icmp header
#include<netinet/udp.h>   //Provides declarations for udp header
#include<netinet/tcp.h>   //Provides declarations for tcp header
#include<netinet/ip.h>    //Provides declarations for ip header
#include<arpa/inet.h>


/* for tcp-communication */
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <string.h>
#include <sys/types.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <netdb.h> 

#include <libnetfilter_queue/libnetfilter_queue.h>


/*  = Netfilter-Bridge client =
 *  Used for notifying DiscoWall about incomming/outgoing packages and querying response (ACCEPT/DROP).
 *  
 *  External sources:
 *  > Thanks goes to "http://www.binarytides.com/packet-sniffer-code-c-linux/" for the utility-methods used for decoding & printing ip-/tcp-package information.
 *  > Also thanks to "Paul Amaranth" (paul@auroragrp.com) for his how-to on the fact that I simply need to cast "unsigned char *data" to "struct iphdr *".
 */


/* ======================================================================================== */
/* Global Variables */
/* ======================================================================================== */

int sockfd; // server (android app) connection
struct sockaddr_in source,dest; // printer-methods

/* ======================================================================================== */
/* Utility-Methods */
/* ======================================================================================== */

void error(const char *msg)
{
    fprintf(stderr, "ERROR: %s\n", msg);
    exit(1);
}


/* ======================================================================================== */
/* Package-Handling */
/* ======================================================================================== */

void print_ip_header(unsigned char* Buffer, int Size)
{
    unsigned short iphdrlen;
         
    struct iphdr *iph = (struct iphdr *)Buffer;
    iphdrlen =iph->ihl*4;
     
    memset(&source, 0, sizeof(source));
    source.sin_addr.s_addr = iph->saddr;
     
    memset(&dest, 0, sizeof(dest));
    dest.sin_addr.s_addr = iph->daddr;
     
    fprintf(stdout,"\n");
    fprintf(stdout,"IP Header\n");
    fprintf(stdout,"   |-IP Version        : %d\n",(unsigned int)iph->version);
    fprintf(stdout,"   |-IP Header Length  : %d DWORDS or %d Bytes\n",(unsigned int)iph->ihl,((unsigned int)(iph->ihl))*4);
    fprintf(stdout,"   |-Type Of Service   : %d\n",(unsigned int)iph->tos);
    fprintf(stdout,"   |-IP Total Length   : %d  Bytes(Size of Packet)\n",ntohs(iph->tot_len));
    fprintf(stdout,"   |-Identification    : %d\n",ntohs(iph->id));
    //fprintf(stdout,"   |-Reserved ZERO Field   : %d\n",(unsigned int)iphdr->ip_reserved_zero);
    //fprintf(stdout,"   |-Dont Fragment Field   : %d\n",(unsigned int)iphdr->ip_dont_fragment);
    //fprintf(stdout,"   |-More Fragment Field   : %d\n",(unsigned int)iphdr->ip_more_fragment);
    fprintf(stdout,"   |-TTL      : %d\n",(unsigned int)iph->ttl);
    fprintf(stdout,"   |-Protocol : %d\n",(unsigned int)iph->protocol);
    fprintf(stdout,"   |-Checksum : %d\n",ntohs(iph->check));
    fprintf(stdout,"   |-Source IP        : %s\n",inet_ntoa(source.sin_addr));
    fprintf(stdout,"   |-Destination IP   : %s\n",inet_ntoa(dest.sin_addr));
}

/* Returns true, if the package should be accepted, false otherwise */
bool handle_tcp_packet(unsigned char* Buffer, int Size)
{
    unsigned short iphdrlen;
     
    struct iphdr *iph = (struct iphdr *)Buffer;
    iphdrlen = iph->ihl*4;
     
    struct tcphdr *tcph=(struct tcphdr*)(Buffer + iphdrlen);
             
    fprintf(stdout,"\n\n***********************TCP Packet*************************\n");    
         
    print_ip_header(Buffer,Size);
         
    fprintf(stdout,"\n");
    fprintf(stdout,"TCP Header\n");
    fprintf(stdout,"   |-Source Port      : %u\n",ntohs(tcph->source));
    fprintf(stdout,"   |-Destination Port : %u\n",ntohs(tcph->dest));
    fprintf(stdout,"   |-Sequence Number    : %u\n",ntohl(tcph->seq));
    fprintf(stdout,"   |-Acknowledge Number : %u\n",ntohl(tcph->ack_seq));
    fprintf(stdout,"   |-Header Length      : %d DWORDS or %d BYTES\n" ,(unsigned int)tcph->doff,(unsigned int)tcph->doff*4);
    //fprintf(stdout,"   |-CWR Flag : %d\n",(unsigned int)tcph->cwr);
    //fprintf(stdout,"   |-ECN Flag : %d\n",(unsigned int)tcph->ece);
    fprintf(stdout,"   |-Urgent Flag          : %d\n",(unsigned int)tcph->urg);
    fprintf(stdout,"   |-Acknowledgement Flag : %d\n",(unsigned int)tcph->ack);
    fprintf(stdout,"   |-Push Flag            : %d\n",(unsigned int)tcph->psh);
    fprintf(stdout,"   |-Reset Flag           : %d\n",(unsigned int)tcph->rst);
    fprintf(stdout,"   |-Synchronise Flag     : %d\n",(unsigned int)tcph->syn);
    fprintf(stdout,"   |-Finish Flag          : %d\n",(unsigned int)tcph->fin);
    fprintf(stdout,"   |-Window         : %d\n",ntohs(tcph->window));
    fprintf(stdout,"   |-Checksum       : %d\n",ntohs(tcph->check));
    fprintf(stdout,"   |-Urgent Pointer : %d\n",tcph->urg_ptr);
    fprintf(stdout,"\n");
    fprintf(stdout,"                        DATA Dump                         ");
    fprintf(stdout,"\n");
         
    fprintf(stdout,"IP Header\n");
    // PrintData(Buffer,iphdrlen);
         
    fprintf(stdout,"TCP Header\n");
    // PrintData(Buffer+iphdrlen,tcph->doff*4);
         
    fprintf(stdout,"Data Payload\n");  
    // PrintData(Buffer + iphdrlen + tcph->doff*4 , (Size - tcph->doff*4-iph->ihl*4) );
                         
    fprintf(stdout,"\n###########################################################");

    return true;
}

 
/* Returns true, if the package should be accepted, false otherwise */
bool handle_udp_packet(unsigned char *Buffer , int Size)
{
     
    unsigned short iphdrlen;
     
    struct iphdr *iph = (struct iphdr *)Buffer;
    iphdrlen = iph->ihl*4;
     
    struct udphdr *udph = (struct udphdr*)(Buffer + iphdrlen);
     
    fprintf(stdout,"\n\n***********************UDP Packet*************************\n");
     
    print_ip_header(Buffer,Size);           
     
    fprintf(stdout,"\nUDP Header\n");
    fprintf(stdout,"   |-Source Port      : %d\n" , ntohs(udph->source));
    fprintf(stdout,"   |-Destination Port : %d\n" , ntohs(udph->dest));
    fprintf(stdout,"   |-UDP Length       : %d\n" , ntohs(udph->len));
    fprintf(stdout,"   |-UDP Checksum     : %d\n" , ntohs(udph->check));
     
    fprintf(stdout,"\n");
    fprintf(stdout,"IP Header\n");
    // PrintData(Buffer , iphdrlen);
         
    fprintf(stdout,"UDP Header\n");
    // PrintData(Buffer+iphdrlen , sizeof udph);
         
    fprintf(stdout,"Data Payload\n");  
    // PrintData(Buffer + iphdrlen + sizeof udph ,( Size - sizeof udph - iph->ihl * 4 ));
     
    fprintf(stdout,"\n###########################################################");

    return true;
}

 
/* ======================================================================================== */
/* Netfilter Stuff */
/* ======================================================================================== */

/* returns packet id */
static u_int32_t handle_pkt(struct nfq_data *tb)
{
	int id = 0;
	struct nfqnl_msg_packet_hdr *ph;
	struct nfqnl_msg_packet_hw *hwph;
	u_int32_t mark,ifi; 
	int data_size;
	unsigned char *data;

	ph = nfq_get_msg_packet_hdr(tb);
	if (ph) {
		id = ntohl(ph->packet_id);
		fprintf(stdout, "hw_protocol=0x%04x hook=%u id=%u ",
			ntohs(ph->hw_protocol), ph->hook, id);
	}

	hwph = nfq_get_packet_hw(tb);
	if (hwph) {
		int i, hlen = ntohs(hwph->hw_addrlen);

		fprintf(stdout, "hw_src_addr=");
		for (i = 0; i < hlen-1; i++)
			fprintf(stdout, "%02x:", hwph->hw_addr[i]);
		fprintf(stdout, "%02x ", hwph->hw_addr[hlen-1]);
	}

	// mark = nfq_get_nfmark(tb);
	// if (mark)
	// 	fprintf(stdout, "mark=%u ", mark);

	// ifi = nfq_get_indev(tb);
	// if (ifi)
	// 	fprintf(stdout, "indev=%u ", ifi);

	// ifi = nfq_get_outdev(tb);
	// if (ifi)
	// 	fprintf(stdout, "outdev=%u ", ifi);
	// ifi = nfq_get_physindev(tb);
	// if (ifi)
	// 	fprintf(stdout, "physindev=%u ", ifi);

	// ifi = nfq_get_physoutdev(tb);
	// if (ifi)
	// 	fprintf(stdout, "physoutdev=%u ", ifi);

	data_size = nfq_get_payload(tb, &data);
	if (data_size >= 0)
		fprintf(stdout, "payload_len=%d ", data_size);

	fputc('\n', stdout);


	// -------------------------------------------------------------------------------
	//    Reading information relevant for firewall decision
	// -------------------------------------------------------------------------------


	// --------------------------------- IP Decoding ---------------------------------

    struct iphdr *ip;
    // struct in_addr ipa;
    char src_ip_str[20];
    char dst_ip_str[20];
	
	// Get IP addresses in char form
	ip = (struct iphdr *) data;

	//// Printing the IP altogether with TCP infos
	// ipa.s_addr=ip->saddr;
	// strcpy (src_ip_str, inet_ntoa(ipa));
	// ipa.s_addr=ip->daddr;
	// strcpy (dst_ip_str, inet_ntoa(ipa));
	// fprintf(stdout, "Source IP: %s   Destination IP: %s\n", src_ip_str, dst_ip_str);
	
	    
	// --------------------------------- TCP/UDP Decoding ---------------------------------
	fprintf(stdout, "Protocol-Type:");

    switch (ip->protocol) //Check the Protocol and do accordingly...
    {
        case 1:  //ICMP Protocol
            fprintf(stdout, "ICMP --> ignoring package.\n");
            break;
         
        case 2:  //IGMP Protocol
            fprintf(stdout, "IGMP --> ignoring package.\n");
            break;
         
        case 6:  //TCP Protocol
        	fprintf(stdout, "TCP --> forwarding info to firewall...\n");
    		handle_tcp_packet(data, data_size);
            break;
         
        case 17: //UDP Protocol
            fprintf(stdout, "UDP --> forwarding info to firewall...\n");
            handle_udp_packet(data, data_size);
            break;
         
        default: //Some Other Protocol like ARP etc.
			fprintf(stdout, "<unknown protocol> --> ignoring package.\n");
            break;
    }

	return id;
}


void sendMessageToServer(char *message)
{
	char buffer[256];
    bzero(buffer,256); // overwrite buffer with zeros
	strcpy(buffer, message);
	fprintf(stdout, "SEND: sending message through channel: %s", buffer);

	// TCP send: write from buffer into socket 
    int n = write(sockfd,buffer,strlen(buffer));

    if (n < 0) 
     error("ERROR writing to socket");
}

	
// CallBack: is being called for each package by nfqueue
static int cb(struct nfq_q_handle *qh, struct nfgenmsg *nfmsg,
	      struct nfq_data *nfa, void *data)
{
	fprintf(stdout, "\n================ Package Received ================\n");
	u_int32_t id = handle_pkt(nfa);

	sendMessageToServer("#COMMENT#package received.\n");

	return nfq_set_verdict(qh, id, NF_ACCEPT, 0, NULL);
	// return nfq_set_verdict(qh, id, NF_DROP, 0, NULL);
}

void startNfqueueCallbacks()
{
	struct nfq_handle *h;
	struct nfq_q_handle *qh;
	struct nfnl_handle *nh;
	int fd;
	int rv;
	char buf[4096] __attribute__ ((aligned));

	fprintf(stdout, "opening library handle\n");
	h = nfq_open();
	if (!h) {
		fprintf(stderr, "error during nfq_open()\n");
		exit(1);
	}

	fprintf(stdout, "unbinding existing nf_queue handler for AF_INET (if any)\n");
	if (nfq_unbind_pf(h, AF_INET) < 0) {
		fprintf(stderr, "error during nfq_unbind_pf()\n");
		exit(1);
	}

	fprintf(stdout, "binding nfnetlink_queue as nf_queue handler for AF_INET\n");
	if (nfq_bind_pf(h, AF_INET) < 0) {
		fprintf(stderr, "error during nfq_bind_pf()\n");
		exit(1);
	}

	fprintf(stdout, "binding this socket to queue '0'\n");
	qh = nfq_create_queue(h,  0, &cb, NULL);
	if (!qh) {
		fprintf(stderr, "error during nfq_create_queue()\n");
		exit(1);
	}

	fprintf(stdout, "setting copy_packet mode\n");
	if (nfq_set_mode(qh, NFQNL_COPY_PACKET, 0xffff) < 0) {
		fprintf(stderr, "can't set packet_copy mode\n");
		exit(1);
	}

	fd = nfq_fd(h);

	for (;;) {
		if ((rv = recv(fd, buf, sizeof(buf), 0)) >= 0) {
			fprintf(stdout, "pkt received\n");
			nfq_handle_packet(h, buf, rv);

			continue;
		}
		/* if your application is too slow to digest the packets that
		 * are sent from kernel-space, the socket buffer that we use
		 * to enqueue packets may fill up returning ENOBUFS. Depending
		 * on your application, this error may be ignored. Please, see
		 * the doxygen documentation of this library on how to improve
		 * this situation.
		 */
		if (rv < 0 && errno == ENOBUFS) {
			fprintf(stdout, "losing packets!\n");
			continue;
		}
		fprintf(stderr,"recv failed");
		break;
	}

	fprintf(stdout, "unbinding from queue 0\n");
	nfq_destroy_queue(qh);

#ifdef INSANE
	/* normally, applications SHOULD NOT issue this command, since
	 * it detaches other programs/sockets from AF_INET, too ! */
	fprintf(stdout, "unbinding from AF_INET\n");
	nfq_unbind_pf(h, AF_INET);
#endif

	fprintf(stdout, "closing library handle\n");
	nfq_close(h);

	exit(0);
	return;
}

/* ======================================================================================== */
/* TCP communication */
/* ======================================================================================== */

void connectToServer(const char *hostname, const char *port)
{
	fprintf(stdout, "Connecting to server %s:%s\n", hostname, port);

    struct sockaddr_in serv_addr;
    struct hostent *server;

    int portno = atoi(port);
    //int sockfd = socket(AF_INET, SOCK_STREAM, 0);
    sockfd = socket(AF_INET, SOCK_STREAM, 0); // declared public
    if (sockfd < 0) 
        error("ERROR opening socket");

    server = gethostbyname(hostname);
    if (server == NULL) {
        error("no such host");
    }

    bzero((char *) &serv_addr, sizeof(serv_addr));
    serv_addr.sin_family = AF_INET;

    bcopy((char *)server->h_addr, 
         (char *)&serv_addr.sin_addr.s_addr,
         server->h_length);
    serv_addr.sin_port = htons(portno);
    
    if (connect(sockfd,(struct sockaddr *) &serv_addr,sizeof(serv_addr)) < 0) 
        error("ERROR connecting");

	fprintf(stdout, "Connected.\n");

	// To send strings via console to server
//    fprintf(stdout, "Please enter the message: ");
//    char buffer[256];
//    bzero(buffer,256);
//    fgets(buffer,255,stdin); // read from stdin and write to buffer

    char buffer[256];
    bzero(buffer,256);
    char *message = "#COMMENT#Netfilter-Bridge says hello.\n";
	strcpy(buffer, message);
	fprintf(stdout, "SEND: sending message through channel: %s", buffer);


	// TCP send: write from buffer into socket 
    int n = write(sockfd,buffer,strlen(buffer));
    fprintf(stdout, "SEND: greeting-message sent.\n");

    if (n < 0) 
         error("ERROR writing to socket");

    bzero(buffer,256); // overwrite buffer with zeros


	// TCP receive: read from tcp stream and write to buffer 
	fprintf(stdout, "RECEIVE: waiting for response...\n");
    n = read(sockfd,buffer,255);
    if (n < 0) 
         error("ERROR reading from socket");
    
    fprintf(stdout, "RECEIVE: response received: %s\n", buffer);
    

//    // TCP: close connection
//    fprintf(stdout, "Closing TCP connection...\n");
//    close(sockfd);
//    fprintf(stdout, "TCP connection closed.\n");
    
    return;
}

void closeConnection() 
{
	// TCP: close connection
    fprintf(stdout, "Closing TCP connection...\n");
    close(sockfd);
    fprintf(stdout, "TCP connection closed.\n");
}

/* ======================================================================================== */

int main(int argc, char **argv)
{
	if (argc < 3) {
       fprintf(stderr,"usage %s hostname port\n", argv[0]);
       exit(0);
    }

	fprintf(stdout, "Netfilter-Bridge: application started...\n");

    connectToServer(argv[1], argv[2]);

	startNfqueueCallbacks();
	closeConnection();

	fprintf(stdout, "Netfilter-Bridge: all done. good bye.\n");

	exit(0);
	return 0;
}


