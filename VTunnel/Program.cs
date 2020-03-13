using System;
using System.IO;
using System.Diagnostics;
using Microsoft.Win32;

using HtmlAgilityPack;

namespace ConnectTest
{
    class Program
    {
        public static string ServerString { get; set; }
        public static string ConfTmpPath { get; set; }

        static void Main(string[] args)
        {
            GetServer();
            WriteTmpConf();
            StartStunnel();
            SetProxy();
            OpenHomePage();
            Console.WriteLine("Press Any Key to Exit;");
            Console.ReadKey();
            ClearProxy();
        }

        private static void GetServer()
        {
            Console.WriteLine("Trying to connect to the server!");
            // From Web
            Uri baseUri = new Uri("https://www.github.com/");
            var url = new Uri(baseUri, "apps/" + "YourApp");

            var web = new HtmlWeb();
            web.UserAgent = "Mozilla/5.0 (Linux; Android 7.0; SM-G930V Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.125 Mobile Safari/537.36";
            var doc = web.Load(url);
            Console.WriteLine("Connecting to the List server. Please Wait for a moment!");

            foreach (HtmlNode link in doc.DocumentNode.SelectNodes("//a[@href]"))
            {
                // Get the value of the HREF attribute
                string hrefValue = link.GetAttributeValue("href", string.Empty);

                if (hrefValue.Contains("amazonaws.com"))
                {
                    ServerString = hrefValue;
                } 

            }
        }

        private static void WriteTmpConf()
        {
            var ProxyServer = new Uri(ServerString);
            string conf =
                "client = yes" + Environment.NewLine
                + "[tinyproxy]" + Environment.NewLine
                + "accept = 127.0.0.1:3128" + Environment.NewLine
                + "connect = " + ProxyServer.Host + ":3128" + Environment.NewLine
                + "CAFile = cert.pem" + Environment.NewLine

                + "[microsocks]" + Environment.NewLine
                + "accept = 127.0.0.1:1082" + Environment.NewLine
                + "connect = " + ProxyServer.Host + ":1082" + Environment.NewLine
                + "CAFile = cert.pem" + Environment.NewLine;
            
            ConfTmpPath = Path.Combine(Directory.GetCurrentDirectory(), "stunnel.conf");

            File.WriteAllText(ConfTmpPath, conf);
        }

        private static void StartStunnel()
        {           
            Process cmd = new Process();
            //https://www.stunnel.org/downloads.html
            cmd.StartInfo.FileName = "stunnel.exe";
            cmd.StartInfo.RedirectStandardInput = true;
            cmd.StartInfo.RedirectStandardOutput = true;
            cmd.StartInfo.Arguments = ConfTmpPath;
            cmd.StartInfo.CreateNoWindow = true;
            cmd.StartInfo.UseShellExecute = false;
            cmd.Start();
        }

        private static void SetProxy()
        {
            RegistryKey registry = Registry.CurrentUser.OpenSubKey("Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings", true);
            registry.SetValue("ProxyEnable", 1);
            registry.SetValue("ProxyServer", "127.0.0.1:3128");
        }

        private static void OpenHomePage()
        {
            Process.Start("https://iplocation.net");
        }

        private static void ClearProxy()
        {
            RegistryKey registry = Registry.CurrentUser.OpenSubKey("Software\\Microsoft\\Windows\\CurrentVersion\\Internet Settings", true);
            registry.SetValue("ProxyEnable", 0);
            registry.SetValue("ProxyServer", 0);

            foreach (var process in Process.GetProcessesByName("stunnel"))
            {
                process.Kill();
            }

            File.Delete(ConfTmpPath);
        }
        
    }
}
