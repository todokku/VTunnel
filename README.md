# VTunnel
Simple TLS Proxy Client


## ReadMe

The standard HTTP header is the rich set of client info, which includes the client's IP and browser's user-agent. This product acts as the proxy client, which provides no leak of client info.

## What's the difference between VTunnel and VPN?

VPN connection hides the IP by the IP of the exit node, however, your ISP can still see the connection type, and where this connection comes from. VTunnel will wrap the network traffic as in the Application Layer Traffic(OSI Layer 7), which appears to the ISP as the standard HTTPS encrypted traffic.

## How to debug the network traffic?

Use TCPDump or WinDump.

##Server Side Configuration

See Link: 
https://web.archive.org/web/20190909122510/https://bencane.com/2017/04/15/using-stunnel-and-tinyproxy-to-hide-http-traffic/

## How to build?

Use build script in the directory of the source code.

## How to run the program?

Start the "VTunnel.exe" command-line binary from the explorer or the command line.
