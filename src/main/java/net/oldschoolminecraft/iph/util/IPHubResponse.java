package net.oldschoolminecraft.iph.util;

public class IPHubResponse
{
    public String ip;
    public String countryCode;
    public String countryName;
    public int asn;
    public String isp;
    public int block;

    public boolean hasNullData()
    {
        if (ip == null) return true;
        if (countryCode == null) return true;
        if (countryName == null) return true;
        if (isp == null) return true;
        return false;
    }
}
