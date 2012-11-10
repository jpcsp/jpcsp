Xuggle-5.4 has instability problems on Windows 64bit platform. This is related to the support of AVX
on 'Sandy Bridge / Ivy Bridge' series of Intel Processors.
See thread
    http://www.emunewz.net/forum/showthread.php?tid=42305
for more details.

Workaround: Use a custom build of Xuggle-5.5 where AVX has been disabled (--disable-avx)
This custom build has been provided by Hyakki, see thread
    http://www.emunewz.net/forum/showthread.php?tid=93353
for more details.
