package arun.com.chromer.parser;

import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import timber.log.Timber;

/**
 * Created by Arunkumar on 16/02/17.
 * An utility class to fetch a website as {@link String}.
 * Adapted from https://github.com/karussell/snacktory by Peter Karich
 */
class WebsiteDownloader {
    private static final String ACCEPT = "application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5";
    // We will spoof as an iPad so that websites properly expose their shortcut icon. Even Google.com
    // does not provide bigger icons when we go as Android.
    private static final String USER_AGENT = "Mozilla/5.0 (iPad; CPU OS 6_0 like Mac OS X) AppleWebKit/536.26 (KHTML, like Gecko) Version/6.0 Mobile/10A5376e Safari/8536.25";

    @WorkerThread
    public static String htmlString(@NonNull final String url) throws IOException {
        final HttpURLConnection urlConnection = createUrlConnection(url, 10000);
        urlConnection.setInstanceFollowRedirects(true);
        final String encoding = urlConnection.getContentEncoding();
        final InputStream inputStream;
        if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
            inputStream = new GZIPInputStream(urlConnection.getInputStream());
        } else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
            inputStream = new InflaterInputStream(urlConnection.getInputStream(), new Inflater(true));
        } else {
            inputStream = urlConnection.getInputStream();
        }
        final String enc = Converter.extractEncoding(urlConnection.getContentType());
        final String result = new Converter(url).streamToString(inputStream, enc);
        urlConnection.disconnect();
        return result;
    }

    @NonNull
    static String unShortenUrl(@NonNull String url) {
        final int maxRedirects = 5;
        String unShortenedUrl = url;
        for (int i = 0; i < maxRedirects; i++) {
            url = getRedirectUrl(url);
            Timber.d("Redirect: %s", url);
            if (url.equalsIgnoreCase(unShortenedUrl)) {
                return unShortenedUrl;
            }
            unShortenedUrl = url;
        }
        return unShortenedUrl;
    }

    private static String getRedirectUrl(@NonNull final String url) {
        HttpURLConnection conn = null;
        try {
            conn = createUrlConnection(url, 10000);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("HEAD");
            conn.connect();
            int responseCode = conn.getResponseCode();
            if (responseCode >= 300 && responseCode < 400) {
                return useDomainOfFirstArg4Second(url, conn.getHeaderField("Location"));
            } else if (responseCode >= 200 && responseCode < 300) {
                return url;
            }
        } catch (Exception ex) {
            return url;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return url;
    }

    private static String useDomainOfFirstArg4Second(String urlForDomain, String path) {
        if (path.startsWith("http"))
            return path;

        if ("favicon.ico".equals(path))
            path = "/favicon.ico";

        if (path.startsWith("//")) {
            // wikipedia special case, see tests
            if (urlForDomain.startsWith("https:"))
                return "https:" + path;

            return "http:" + path;
        } else if (path.startsWith("/"))
            return "http://" + extractHost(urlForDomain) + path;
        else if (path.startsWith("../")) {
            final int slashIndex = urlForDomain.lastIndexOf("/");
            if (slashIndex > 0 && slashIndex + 1 < urlForDomain.length())
                urlForDomain = urlForDomain.substring(0, slashIndex + 1);
            return urlForDomain + path;
        }
        return path;
    }

    private static String extractHost(String url) {
        return extractDomain(url, false);
    }

    private static String extractDomain(String url, boolean aggressive) {
        if (url.startsWith("http://"))
            url = url.substring("http://".length());
        else if (url.startsWith("https://"))
            url = url.substring("https://".length());

        if (aggressive) {
            if (url.startsWith("www."))
                url = url.substring("www.".length());

            // strip mobile from start
            if (url.startsWith("m."))
                url = url.substring("m.".length());
        }

        int slashIndex = url.indexOf("/");
        if (slashIndex > 0)
            url = url.substring(0, slashIndex);

        return url;
    }

    private static HttpURLConnection createUrlConnection(String urlAsStr, int timeout) throws IOException {
        final URL url = new URL(urlAsStr);
        //using proxy may increase latency
        final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestProperty("User-Agent", USER_AGENT);
        urlConnection.setRequestProperty("Accept", ACCEPT);
        // suggest respond to be gzipped or deflated (which is just another compression)
        // http://stackoverflow.com/q/3932117
        urlConnection.setRequestProperty("Accept-Encoding", "gzip, deflate");
        urlConnection.setConnectTimeout(timeout);
        urlConnection.setReadTimeout(timeout);
        return urlConnection;
    }

    private static class Converter {
        final static String UTF8 = "UTF-8";
        final static String ISO = "ISO-8859-1";
        final static int K2 = 2048;
        private int maxBytes = 1000000 / 2;
        private String encoding;
        private String url;

        Converter(String urlOnlyHint) {
            url = urlOnlyHint;
        }

        Converter() {
        }

        public Converter setMaxBytes(int maxBytes) {
            this.maxBytes = maxBytes;
            return this;
        }

        static String extractEncoding(String contentType) {
            final String[] values;
            if (contentType != null)
                values = contentType.split(";");
            else
                values = new String[0];
            String charset = "";

            for (String value : values) {
                value = value.trim().toLowerCase();

                if (value.startsWith("charset="))
                    charset = value.substring("charset=".length());
            }
            // http1.1 says ISO-8859-1 is the default charset
            if (charset.length() == 0)
                charset = ISO;
            return charset;
        }

        public String getEncoding() {
            if (encoding == null)
                return "";
            return encoding.toLowerCase();
        }

        public String streamToString(InputStream is) {
            return streamToString(is, maxBytes, encoding);
        }

        String streamToString(InputStream is, String enc) {
            return streamToString(is, maxBytes, enc);
        }

        /**
         * reads bytes off the string and returns a string
         *
         * @param is
         * @param maxBytes The max bytes that we want to read from the input stream
         * @return String
         */
        String streamToString(InputStream is, int maxBytes, String encoding) {
            this.encoding = encoding;
            // Http 1.1. standard is iso-8859-1 not utf8 :(
            // but we force utf-8 as youtube assumes it ;)
            if (this.encoding == null || this.encoding.isEmpty())
                this.encoding = UTF8;

            BufferedInputStream in = null;
            try {
                in = new BufferedInputStream(is, K2);
                final ByteArrayOutputStream output = new ByteArrayOutputStream();

                // detect encoding with the help of meta tag
                try {
                    in.mark(K2 * 2);
                    String tmpEnc = detectCharset("charset=", output, in, this.encoding);
                    if (tmpEnc != null)
                        this.encoding = tmpEnc;
                    else {
                        Timber.d("no charset found in first stage");
                        // detect with the help of xml beginning ala encoding="charset"
                        tmpEnc = detectCharset("encoding=", output, in, this.encoding);
                        if (tmpEnc != null)
                            this.encoding = tmpEnc;
                        else
                            Timber.d("no charset found in second stage");
                    }
                    if (!Charset.isSupported(this.encoding))
                        throw new UnsupportedEncodingException(this.encoding);
                } catch (UnsupportedEncodingException e) {
                    Timber.e(e, "Using default encoding:%s encoding %s", encoding, url);
                    this.encoding = UTF8;
                }

                // SocketException: Connection reset
                // IOException: missing CR    => problem on server (probably some xml character thing?)
                // IOException: Premature EOF => socket unexpectly closed from server
                int bytesRead = output.size();
                byte[] arr = new byte[K2];
                while (true) {
                    if (bytesRead >= maxBytes) {
                        Timber.w("Maxbyte of %d exceeded! Maybe html is now broken but try it nevertheless. Url: %s ", maxBytes, url);
                        break;
                    }

                    int n = in.read(arr);
                    if (n < 0)
                        break;
                    bytesRead += n;
                    output.write(arr, 0, n);
                }

                return output.toString(this.encoding);
            } catch (IOException e) {
                Timber.e(e, " url: %s", url);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception ignored) {
                    }
                }
            }
            return "";
        }

        /**
         * This method detects the charset even if the first call only returns some
         * bytes. It will read until 4K bytes are reached and then try to determine
         * the encoding
         *
         * @throws IOException
         */
        String detectCharset(String key, ByteArrayOutputStream bos, BufferedInputStream in,
                             String enc) throws IOException {
            // Grab better encoding from stream
            byte[] arr = new byte[K2];
            int nSum = 0;
            while (nSum < K2) {
                int n = in.read(arr);
                if (n < 0)
                    break;

                nSum += n;
                bos.write(arr, 0, n);
            }

            String str = bos.toString(enc);
            int encIndex = str.indexOf(key);
            int clength = key.length();
            if (encIndex > 0) {
                char startChar = str.charAt(encIndex + clength);
                int lastEncIndex;
                if (startChar == '\'')
                    // if we have charset='something'
                    lastEncIndex = str.indexOf("'", ++encIndex + clength);
                else if (startChar == '\"')
                    // if we have charset="something"
                    lastEncIndex = str.indexOf("\"", ++encIndex + clength);
                else {
                    // if we have "text/html; charset=utf-8"
                    int first = str.indexOf("\"", encIndex + clength);
                    if (first < 0)
                        first = Integer.MAX_VALUE;

                    // or "text/html; charset=utf-8 "
                    int sec = str.indexOf(" ", encIndex + clength);
                    if (sec < 0)
                        sec = Integer.MAX_VALUE;
                    lastEncIndex = Math.min(first, sec);

                    // or "text/html; charset=utf-8 '
                    int third = str.indexOf("'", encIndex + clength);
                    if (third > 0)
                        lastEncIndex = Math.min(lastEncIndex, third);
                }

                // re-read byte array with different encoding
                // assume that the encoding string cannot be greater than 40 chars
                if (lastEncIndex > encIndex + clength && lastEncIndex < encIndex + clength + 40) {
                    String tmpEnc = encodingCleanup(str.substring(encIndex + clength, lastEncIndex));
                    try {
                        in.reset();
                        bos.reset();
                        return tmpEnc;
                    } catch (IOException ex) {
                        Timber.w(enc, "Couldn't reset stream to re-read with new encoding");
                    }
                }
            }
            return null;
        }

        static String encodingCleanup(final String str) {
            final StringBuilder sb = new StringBuilder();
            boolean startedWithCorrectString = false;
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if (Character.isDigit(c) || Character.isLetter(c) || c == '-' || c == '_') {
                    startedWithCorrectString = true;
                    sb.append(c);
                    continue;
                }

                if (startedWithCorrectString)
                    break;
            }
            return sb.toString().trim();
        }
    }
}
