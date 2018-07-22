(function () {
    function r(e, n, t) {
        function o(i, f) {
            if (!n[i]) {
                if (!e[i]) {
                    var c = "function" == typeof require && require;
                    if (!f && c) return c(i, !0);
                    if (u) return u(i, !0);
                    var a = new Error("Cannot find module '" + i + "'");
                    throw a.code = "MODULE_NOT_FOUND", a
                }
                var p = n[i] = {exports: {}};
                e[i][0].call(p.exports, function (r) {
                    var n = e[i][1][r];
                    return o(n || r)
                }, p, p.exports, r, e, n, t)
            }
            return n[i].exports
        }

        for (var u = "function" == typeof require && require, i = 0; i < t.length; i++) o(t[i]);
        return o
    }

    return r
})()({
    1: [function (require, module, exports) {
    module.rectangle_decomposition = require("rectangle-decomposition")
// print(rectangle_decomposition([[1,1]]))
    }, {"rectangle-decomposition": 13}], 2: [function (require, module, exports) {
        'use strict'

        exports.byteLength = byteLength
        exports.toByteArray = toByteArray
        exports.fromByteArray = fromByteArray

        var lookup = []
        var revLookup = []
        var Arr = typeof Uint8Array !== 'undefined' ? Uint8Array : Array

        var code = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/'
        for (var i = 0, len = code.length; i < len; ++i) {
            lookup[i] = code[i]
            revLookup[code.charCodeAt(i)] = i
        }

// Support decoding URL-safe base64 strings, as Node.js does.
// See: https://en.wikipedia.org/wiki/Base64#URL_applications
        revLookup['-'.charCodeAt(0)] = 62
        revLookup['_'.charCodeAt(0)] = 63

        function getLens(b64) {
            var len = b64.length

            if (len % 4 > 0) {
                throw new Error('Invalid string. Length must be a multiple of 4')
            }

            // Trim off extra bytes after placeholder bytes are found
            // See: https://github.com/beatgammit/base64-js/issues/42
            var validLen = b64.indexOf('=')
            if (validLen === -1) validLen = len

            var placeHoldersLen = validLen === len
                ? 0
                : 4 - (validLen % 4)

            return [validLen, placeHoldersLen]
        }

// base64 is 4/3 + up to two characters of the original data
        function byteLength(b64) {
            var lens = getLens(b64)
            var validLen = lens[0]
            var placeHoldersLen = lens[1]
            return ((validLen + placeHoldersLen) * 3 / 4) - placeHoldersLen
        }

        function _byteLength(b64, validLen, placeHoldersLen) {
            return ((validLen + placeHoldersLen) * 3 / 4) - placeHoldersLen
        }

        function toByteArray(b64) {
            var tmp
            var lens = getLens(b64)
            var validLen = lens[0]
            var placeHoldersLen = lens[1]

            var arr = new Arr(_byteLength(b64, validLen, placeHoldersLen))

            var curByte = 0

            // if there are placeholders, only get up to the last complete 4 chars
            var len = placeHoldersLen > 0
                ? validLen - 4
                : validLen

            for (var i = 0; i < len; i += 4) {
                tmp =
                    (revLookup[b64.charCodeAt(i)] << 18) |
                    (revLookup[b64.charCodeAt(i + 1)] << 12) |
                    (revLookup[b64.charCodeAt(i + 2)] << 6) |
                    revLookup[b64.charCodeAt(i + 3)]
                arr[curByte++] = (tmp >> 16) & 0xFF
                arr[curByte++] = (tmp >> 8) & 0xFF
                arr[curByte++] = tmp & 0xFF
            }

            if (placeHoldersLen === 2) {
                tmp =
                    (revLookup[b64.charCodeAt(i)] << 2) |
                    (revLookup[b64.charCodeAt(i + 1)] >> 4)
                arr[curByte++] = tmp & 0xFF
            }

            if (placeHoldersLen === 1) {
                tmp =
                    (revLookup[b64.charCodeAt(i)] << 10) |
                    (revLookup[b64.charCodeAt(i + 1)] << 4) |
                    (revLookup[b64.charCodeAt(i + 2)] >> 2)
                arr[curByte++] = (tmp >> 8) & 0xFF
                arr[curByte++] = tmp & 0xFF
            }

            return arr
        }

        function tripletToBase64(num) {
            return lookup[num >> 18 & 0x3F] +
                lookup[num >> 12 & 0x3F] +
                lookup[num >> 6 & 0x3F] +
                lookup[num & 0x3F]
        }

        function encodeChunk(uint8, start, end) {
            var tmp
            var output = []
            for (var i = start; i < end; i += 3) {
                tmp =
                    ((uint8[i] << 16) & 0xFF0000) +
                    ((uint8[i + 1] << 8) & 0xFF00) +
                    (uint8[i + 2] & 0xFF)
                output.push(tripletToBase64(tmp))
            }
            return output.join('')
        }

        function fromByteArray(uint8) {
            var tmp
            var len = uint8.length
            var extraBytes = len % 3 // if we have 1 byte left, pad 2 bytes
            var parts = []
            var maxChunkLength = 16383 // must be multiple of 3

            // go through the array every three bytes, we'll deal with trailing stuff later
            for (var i = 0, len2 = len - extraBytes; i < len2; i += maxChunkLength) {
                parts.push(encodeChunk(
                    uint8, i, (i + maxChunkLength) > len2 ? len2 : (i + maxChunkLength)
                ))
            }

            // pad the end with zeros, but make sure to not forget the extra bytes
            if (extraBytes === 1) {
                tmp = uint8[len - 1]
                parts.push(
                    lookup[tmp >> 2] +
                    lookup[(tmp << 4) & 0x3F] +
                    '=='
                )
            } else if (extraBytes === 2) {
                tmp = (uint8[len - 2] << 8) + uint8[len - 1]
                parts.push(
                    lookup[tmp >> 10] +
                    lookup[(tmp >> 4) & 0x3F] +
                    lookup[(tmp << 2) & 0x3F] +
                    '='
                )
            }

            return parts.join('')
        }

    }, {}], 3: [function (require, module, exports) {
        "use strict"

        function compileSearch(funcName, predicate, reversed, extraArgs, useNdarray, earlyOut) {
            var code = [
                "function ", funcName, "(a,l,h,", extraArgs.join(","), "){",
                earlyOut ? "" : "var i=", (reversed ? "l-1" : "h+1"),
                ";while(l<=h){\
var m=(l+h)>>>1,x=a", useNdarray ? ".get(m)" : "[m]"]
            if (earlyOut) {
                if (predicate.indexOf("c") < 0) {
                    code.push(";if(x===y){return m}else if(x<=y){")
                } else {
                    code.push(";var p=c(x,y);if(p===0){return m}else if(p<=0){")
                }
            } else {
                code.push(";if(", predicate, "){i=m;")
            }
            if (reversed) {
                code.push("l=m+1}else{h=m-1}")
            } else {
                code.push("h=m-1}else{l=m+1}")
            }
            code.push("}")
            if (earlyOut) {
                code.push("return -1};")
            } else {
                code.push("return i};")
            }
            return code.join("")
        }

        function compileBoundsSearch(predicate, reversed, suffix, earlyOut) {
            var result = new Function([
                compileSearch("A", "x" + predicate + "y", reversed, ["y"], false, earlyOut),
                compileSearch("B", "x" + predicate + "y", reversed, ["y"], true, earlyOut),
                compileSearch("P", "c(x,y)" + predicate + "0", reversed, ["y", "c"], false, earlyOut),
                compileSearch("Q", "c(x,y)" + predicate + "0", reversed, ["y", "c"], true, earlyOut),
                "function dispatchBsearch", suffix, "(a,y,c,l,h){\
if(a.shape){\
if(typeof(c)==='function'){\
return Q(a,(l===undefined)?0:l|0,(h===undefined)?a.shape[0]-1:h|0,y,c)\
}else{\
return B(a,(c===undefined)?0:c|0,(l===undefined)?a.shape[0]-1:l|0,y)\
}}else{\
if(typeof(c)==='function'){\
return P(a,(l===undefined)?0:l|0,(h===undefined)?a.length-1:h|0,y,c)\
}else{\
return A(a,(c===undefined)?0:c|0,(l===undefined)?a.length-1:l|0,y)\
}}}\
return dispatchBsearch", suffix].join(""))
            return result()
        }

        module.exports = {
            ge: compileBoundsSearch(">=", false, "GE"),
            gt: compileBoundsSearch(">", false, "GT"),
            lt: compileBoundsSearch("<", true, "LT"),
            le: compileBoundsSearch("<=", true, "LE"),
            eq: compileBoundsSearch("-", true, "EQ", true)
        }

    }, {}], 4: [function (require, module, exports) {
        "use strict"

        var vertexCover = require("bipartite-vertex-cover")

        module.exports = bipartiteIndependentSet

        function compareInt(a, b) {
            return a - b
        }

        function complement(list, n) {
            var k = list.length
            var result = new Array(n - k)
            var a = 0
            var b = 0
            list.sort(compareInt)
            for (var i = 0; i < n; ++i) {
                if (list[a] === i) {
                    a += 1
                } else {
                    result[b++] = i
                }
            }
            return result
        }

        function bipartiteIndependentSet(n, m, edges) {
            var cover = vertexCover(n, m, edges)
            return [complement(cover[0], n), complement(cover[1], m)]
        }
    }, {"bipartite-vertex-cover": 6}], 5: [function (require, module, exports) {
        "use strict"

        var pool = require("typedarray-pool")
        var INF = (1 << 28)

        module.exports = bipartiteMatching

        function bipartiteMatching(n, m, edges) {

            //Initalize adjacency list, visit flag, distance
            var adjN = new Array(n)
            var g1 = pool.mallocInt32(n)
            var dist = pool.mallocInt32(n)
            for (var i = 0; i < n; ++i) {
                g1[i] = -1
                adjN[i] = []
                dist[i] = INF
            }
            var adjM = new Array(m)
            var g2 = pool.mallocInt32(m)
            for (var i = 0; i < m; ++i) {
                g2[i] = -1
                adjM[i] = []
            }

            //Build adjacency matrix
            var E = edges.length
            for (var i = 0; i < E; ++i) {
                var e = edges[i]
                adjN[e[0]].push(e[1])
                adjM[e[1]].push(e[0])
            }

            var dmax = INF

            function dfs(v) {
                if (v < 0) {
                    return true
                }
                var adj = adjN[v]
                for (var i = 0, l = adj.length; i < l; ++i) {
                    var u = adj[i]
                    var pu = g2[u]
                    var dpu = dmax
                    if (pu >= 0) {
                        dpu = dist[pu]
                    }
                    if (dpu === dist[v] + 1) {
                        if (dfs(pu)) {
                            g1[v] = u
                            g2[u] = v
                            return true
                        }
                    }
                }
                dist[v] = INF
                return false
            }

            //Run search
            var toVisit = pool.mallocInt32(n)
            var matching = 0
            while (true) {

                //Initialize queue
                var count = 0
                for (var i = 0; i < n; ++i) {
                    if (g1[i] < 0) {
                        dist[i] = 0
                        toVisit[count++] = i
                    } else {
                        dist[i] = INF
                    }
                }

                //Run BFS
                var ptr = 0
                dmax = INF
                while (ptr < count) {
                    var v = toVisit[ptr++]
                    var dv = dist[v]
                    if (dv < dmax) {
                        var adj = adjN[v]
                        for (var j = 0, l = adj.length; j < l; ++j) {
                            var u = adj[j]
                            var pu = g2[u]
                            if (pu < 0) {
                                if (dmax === INF) {
                                    dmax = dv + 1
                                }
                            } else if (dist[pu] === INF) {
                                dist[pu] = dv + 1
                                toVisit[count++] = pu
                            }
                        }
                    }
                }


                //Check for termination
                if (dmax === INF) {
                    break
                }

                //Run DFS on each vertex in N
                for (var i = 0; i < n; ++i) {
                    if (g1[i] < 0) {
                        if (dfs(i)) {
                            matching += 1
                        }
                    }
                }
            }

            //Construct result
            var count = 0
            var result = new Array(matching)
            for (var i = 0; i < n; ++i) {
                if (g1[i] < 0) {
                    continue
                }
                result[count++] = [i, g1[i]]
            }

            //Clean up
            pool.free(toVisit)
            pool.free(g2)
            pool.free(dist)
            pool.free(g1)

            //Return
            return result
        }
    }, {"typedarray-pool": 14}], 6: [function (require, module, exports) {
        "use strict"

        var pool = require("typedarray-pool")
        var iota = require("iota-array")
        var bipartiteMatching = require("bipartite-matching")

        module.exports = bipartiteVertexCover

        function walk(list, v, adjL, matchL, coverL, matchR, coverR) {
            if (coverL[v] || matchL[v] >= 0) {
                return
            }
            while (v >= 0) {
                coverL[v] = 1
                var adj = adjL[v]
                var next = -1
                for (var i = 0, l = adj.length; i < l; ++i) {
                    var u = adj[i]
                    if (coverR[u]) {
                        continue
                    }
                    next = u
                }
                if (next < 0) {
                    break
                }
                coverR[next] = 1
                list.push(next)
                v = matchR[next]
            }
        }

        function bipartiteVertexCover(n, m, edges) {
            var match = bipartiteMatching(n, m, edges)

            //Initialize adjacency lists
            var adjL = new Array(n)
            var matchL = pool.mallocInt32(n)
            var matchCount = pool.mallocInt32(n)
            var coverL = pool.mallocInt32(n)
            for (var i = 0; i < n; ++i) {
                adjL[i] = []
                matchL[i] = -1
                matchCount[i] = 0
                coverL[i] = 0
            }
            var adjR = new Array(m)
            var matchR = pool.mallocInt32(m)
            var coverR = pool.mallocInt32(m)
            for (var i = 0; i < m; ++i) {
                adjR[i] = []
                matchR[i] = -1
                coverR[i] = 0
            }

            //Unpack matching
            for (var i = 0, l = match.length; i < l; ++i) {
                var s = match[i][0]
                var t = match[i][1]
                matchL[s] = t
                matchR[t] = s
            }

            //Loop over edges
            for (var i = 0, l = edges.length; i < l; ++i) {
                var e = edges[i]
                var s = e[0]
                var t = e[1]
                if (matchL[s] === t) {
                    if (!(matchCount[s]++)) {
                        continue
                    }
                }
                adjL[s].push(t)
                adjR[t].push(s)
            }

            //Construct cover
            var left = []
            var right = []
            for (var i = 0; i < n; ++i) {
                walk(right, i, adjL, matchL, coverL, matchR, coverR)
            }
            for (var i = 0; i < m; ++i) {
                walk(left, i, adjR, matchR, coverR, matchL, coverL)
            }

            //Clean up any left over edges
            for (var i = 0; i < n; ++i) {
                if (!coverL[i] && matchL[i] >= 0) {
                    coverR[matchL[i]] = coverL[i] = 1
                    left.push(i)
                }
            }

            //Clean up data
            pool.free(coverR)
            pool.free(matchR)
            pool.free(coverL)
            pool.free(matchCount)
            pool.free(matchL)

            return [left, right]
        }
    }, {"bipartite-matching": 5, "iota-array": 12, "typedarray-pool": 14}], 7: [function (require, module, exports) {
        /**
         * Bit twiddling hacks for JavaScript.
         *
         * Author: Mikola Lysenko
         *
         * Ported from Stanford bit twiddling hack library:
         *    http://graphics.stanford.edu/~seander/bithacks.html
         */

        "use strict";
        "use restrict";

//Number of bits in an integer
        var INT_BITS = 32;

//Constants
        exports.INT_BITS = INT_BITS;
        exports.INT_MAX = 0x7fffffff;
        exports.INT_MIN = -1 << (INT_BITS - 1);

//Returns -1, 0, +1 depending on sign of x
        exports.sign = function (v) {
            return (v > 0) - (v < 0);
        }

//Computes absolute value of integer
        exports.abs = function (v) {
            var mask = v >> (INT_BITS - 1);
            return (v ^ mask) - mask;
        }

//Computes minimum of integers x and y
        exports.min = function (x, y) {
            return y ^ ((x ^ y) & -(x < y));
        }

//Computes maximum of integers x and y
        exports.max = function (x, y) {
            return x ^ ((x ^ y) & -(x < y));
        }

//Checks if a number is a power of two
        exports.isPow2 = function (v) {
            return !(v & (v - 1)) && (!!v);
        }

//Computes log base 2 of v
        exports.log2 = function (v) {
            var r, shift;
            r = (v > 0xFFFF) << 4;
            v >>>= r;
            shift = (v > 0xFF) << 3;
            v >>>= shift;
            r |= shift;
            shift = (v > 0xF) << 2;
            v >>>= shift;
            r |= shift;
            shift = (v > 0x3) << 1;
            v >>>= shift;
            r |= shift;
            return r | (v >> 1);
        }

//Computes log base 10 of v
        exports.log10 = function (v) {
            return (v >= 1000000000) ? 9 : (v >= 100000000) ? 8 : (v >= 10000000) ? 7 :
                (v >= 1000000) ? 6 : (v >= 100000) ? 5 : (v >= 10000) ? 4 :
                    (v >= 1000) ? 3 : (v >= 100) ? 2 : (v >= 10) ? 1 : 0;
        }

//Counts number of bits
        exports.popCount = function (v) {
            v = v - ((v >>> 1) & 0x55555555);
            v = (v & 0x33333333) + ((v >>> 2) & 0x33333333);
            return ((v + (v >>> 4) & 0xF0F0F0F) * 0x1010101) >>> 24;
        }

//Counts number of trailing zeros
        function countTrailingZeros(v) {
            var c = 32;
            v &= -v;
            if (v) c--;
            if (v & 0x0000FFFF) c -= 16;
            if (v & 0x00FF00FF) c -= 8;
            if (v & 0x0F0F0F0F) c -= 4;
            if (v & 0x33333333) c -= 2;
            if (v & 0x55555555) c -= 1;
            return c;
        }

        exports.countTrailingZeros = countTrailingZeros;

//Rounds to next power of 2
        exports.nextPow2 = function (v) {
            v += v === 0;
            --v;
            v |= v >>> 1;
            v |= v >>> 2;
            v |= v >>> 4;
            v |= v >>> 8;
            v |= v >>> 16;
            return v + 1;
        }

//Rounds down to previous power of 2
        exports.prevPow2 = function (v) {
            v |= v >>> 1;
            v |= v >>> 2;
            v |= v >>> 4;
            v |= v >>> 8;
            v |= v >>> 16;
            return v - (v >>> 1);
        }

//Computes parity of word
        exports.parity = function (v) {
            v ^= v >>> 16;
            v ^= v >>> 8;
            v ^= v >>> 4;
            v &= 0xf;
            return (0x6996 >>> v) & 1;
        }

        var REVERSE_TABLE = new Array(256);

        (function (tab) {
            for (var i = 0; i < 256; ++i) {
                var v = i, r = i, s = 7;
                for (v >>>= 1; v; v >>>= 1) {
                    r <<= 1;
                    r |= v & 1;
                    --s;
                }
                tab[i] = (r << s) & 0xff;
            }
        })(REVERSE_TABLE);

//Reverse bits in a 32 bit word
        exports.reverse = function (v) {
            return (REVERSE_TABLE[v & 0xff] << 24) |
                (REVERSE_TABLE[(v >>> 8) & 0xff] << 16) |
                (REVERSE_TABLE[(v >>> 16) & 0xff] << 8) |
                REVERSE_TABLE[(v >>> 24) & 0xff];
        }

//Interleave bits of 2 coordinates with 16 bits.  Useful for fast quadtree codes
        exports.interleave2 = function (x, y) {
            x &= 0xFFFF;
            x = (x | (x << 8)) & 0x00FF00FF;
            x = (x | (x << 4)) & 0x0F0F0F0F;
            x = (x | (x << 2)) & 0x33333333;
            x = (x | (x << 1)) & 0x55555555;

            y &= 0xFFFF;
            y = (y | (y << 8)) & 0x00FF00FF;
            y = (y | (y << 4)) & 0x0F0F0F0F;
            y = (y | (y << 2)) & 0x33333333;
            y = (y | (y << 1)) & 0x55555555;

            return x | (y << 1);
        }

//Extracts the nth interleaved component
        exports.deinterleave2 = function (v, n) {
            v = (v >>> n) & 0x55555555;
            v = (v | (v >>> 1)) & 0x33333333;
            v = (v | (v >>> 2)) & 0x0F0F0F0F;
            v = (v | (v >>> 4)) & 0x00FF00FF;
            v = (v | (v >>> 16)) & 0x000FFFF;
            return (v << 16) >> 16;
        }


//Interleave bits of 3 coordinates, each with 10 bits.  Useful for fast octree codes
        exports.interleave3 = function (x, y, z) {
            x &= 0x3FF;
            x = (x | (x << 16)) & 4278190335;
            x = (x | (x << 8)) & 251719695;
            x = (x | (x << 4)) & 3272356035;
            x = (x | (x << 2)) & 1227133513;

            y &= 0x3FF;
            y = (y | (y << 16)) & 4278190335;
            y = (y | (y << 8)) & 251719695;
            y = (y | (y << 4)) & 3272356035;
            y = (y | (y << 2)) & 1227133513;
            x |= (y << 1);

            z &= 0x3FF;
            z = (z | (z << 16)) & 4278190335;
            z = (z | (z << 8)) & 251719695;
            z = (z | (z << 4)) & 3272356035;
            z = (z | (z << 2)) & 1227133513;

            return x | (z << 2);
        }

//Extracts nth interleaved component of a 3-tuple
        exports.deinterleave3 = function (v, n) {
            v = (v >>> n) & 1227133513;
            v = (v | (v >>> 2)) & 3272356035;
            v = (v | (v >>> 4)) & 251719695;
            v = (v | (v >>> 8)) & 4278190335;
            v = (v | (v >>> 16)) & 0x3FF;
            return (v << 22) >> 22;
        }

//Computes next combination in colexicographic order (this is mistakenly called nextPermutation on the bit twiddling hacks page)
        exports.nextCombination = function (v) {
            var t = v | (v - 1);
            return (t + 1) | (((~t & -~t) - 1) >>> (countTrailingZeros(v) + 1));
        }


    }, {}], 8: [function (require, module, exports) {
        /*!
 * The buffer module from node.js, for the browser.
 *
 * @author   Feross Aboukhadijeh <https://feross.org>
 * @license  MIT
 */
        /* eslint-disable no-proto */

        'use strict'

        var base64 = require('base64-js')
        var ieee754 = require('ieee754')

        exports.Buffer = Buffer
        exports.SlowBuffer = SlowBuffer
        exports.INSPECT_MAX_BYTES = 50

        var K_MAX_LENGTH = 0x7fffffff
        exports.kMaxLength = K_MAX_LENGTH

        /**
         * If `Buffer.TYPED_ARRAY_SUPPORT`:
         *   === true    Use Uint8Array implementation (fastest)
         *   === false   Print warning and recommend using `buffer` v4.x which has an Object
         *               implementation (most compatible, even IE6)
         *
         * Browsers that support typed arrays are IE 10+, Firefox 4+, Chrome 7+, Safari 5.1+,
         * Opera 11.6+, iOS 4.2+.
         *
         * We report that the browser does not support typed arrays if the are not subclassable
         * using __proto__. Firefox 4-29 lacks support for adding new properties to `Uint8Array`
         * (See: https://bugzilla.mozilla.org/show_bug.cgi?id=695438). IE 10 lacks support
         * for __proto__ and has a buggy typed array implementation.
         */
        Buffer.TYPED_ARRAY_SUPPORT = typedArraySupport()

        if (!Buffer.TYPED_ARRAY_SUPPORT && typeof console !== 'undefined' &&
            typeof console.error === 'function') {
            console.error(
                'This browser lacks typed array (Uint8Array) support which is required by ' +
                '`buffer` v5.x. Use `buffer` v4.x if you require old browser support.'
            )
        }

        function typedArraySupport() {
            // Can typed array instances can be augmented?
            try {
                var arr = new Uint8Array(1)
                arr.__proto__ = {
                    __proto__: Uint8Array.prototype, foo: function () {
                        return 42
                    }
                }
                return arr.foo() === 42
            } catch (e) {
                return false
            }
        }

        Object.defineProperty(Buffer.prototype, 'parent', {
            get: function () {
                if (!(this instanceof Buffer)) {
                    return undefined
                }
                return this.buffer
            }
        })

        Object.defineProperty(Buffer.prototype, 'offset', {
            get: function () {
                if (!(this instanceof Buffer)) {
                    return undefined
                }
                return this.byteOffset
            }
        })

        function createBuffer(length) {
            if (length > K_MAX_LENGTH) {
                throw new RangeError('Invalid typed array length')
            }
            // Return an augmented `Uint8Array` instance
            var buf = new Uint8Array(length)
            buf.__proto__ = Buffer.prototype
            return buf
        }

        /**
         * The Buffer constructor returns instances of `Uint8Array` that have their
         * prototype changed to `Buffer.prototype`. Furthermore, `Buffer` is a subclass of
         * `Uint8Array`, so the returned instances will have all the node `Buffer` methods
         * and the `Uint8Array` methods. Square bracket notation works as expected -- it
         * returns a single octet.
         *
         * The `Uint8Array` prototype remains unmodified.
         */

        function Buffer(arg, encodingOrOffset, length) {
            // Common case.
            if (typeof arg === 'number') {
                if (typeof encodingOrOffset === 'string') {
                    throw new Error(
                        'If encoding is specified then the first argument must be a string'
                    )
                }
                return allocUnsafe(arg)
            }
            return from(arg, encodingOrOffset, length)
        }

// Fix subarray() in ES2016. See: https://github.com/feross/buffer/pull/97
        if (typeof Symbol !== 'undefined' && Symbol.species &&
            Buffer[Symbol.species] === Buffer) {
            Object.defineProperty(Buffer, Symbol.species, {
                value: null,
                configurable: true,
                enumerable: false,
                writable: false
            })
        }

        Buffer.poolSize = 8192 // not used by this implementation

        function from(value, encodingOrOffset, length) {
            if (typeof value === 'number') {
                throw new TypeError('"value" argument must not be a number')
            }

            if (isArrayBuffer(value) || (value && isArrayBuffer(value.buffer))) {
                return fromArrayBuffer(value, encodingOrOffset, length)
            }

            if (typeof value === 'string') {
                return fromString(value, encodingOrOffset)
            }

            return fromObject(value)
        }

        /**
         * Functionally equivalent to Buffer(arg, encoding) but throws a TypeError
         * if value is a number.
         * Buffer.from(str[, encoding])
         * Buffer.from(array)
         * Buffer.from(buffer)
         * Buffer.from(arrayBuffer[, byteOffset[, length]])
         **/
        Buffer.from = function (value, encodingOrOffset, length) {
            return from(value, encodingOrOffset, length)
        }

// Note: Change prototype *after* Buffer.from is defined to workaround Chrome bug:
// https://github.com/feross/buffer/pull/148
        Buffer.prototype.__proto__ = Uint8Array.prototype
        Buffer.__proto__ = Uint8Array

        function assertSize(size) {
            if (typeof size !== 'number') {
                throw new TypeError('"size" argument must be of type number')
            } else if (size < 0) {
                throw new RangeError('"size" argument must not be negative')
            }
        }

        function alloc(size, fill, encoding) {
            assertSize(size)
            if (size <= 0) {
                return createBuffer(size)
            }
            if (fill !== undefined) {
                // Only pay attention to encoding if it's a string. This
                // prevents accidentally sending in a number that would
                // be interpretted as a start offset.
                return typeof encoding === 'string'
                    ? createBuffer(size).fill(fill, encoding)
                    : createBuffer(size).fill(fill)
            }
            return createBuffer(size)
        }

        /**
         * Creates a new filled Buffer instance.
         * alloc(size[, fill[, encoding]])
         **/
        Buffer.alloc = function (size, fill, encoding) {
            return alloc(size, fill, encoding)
        }

        function allocUnsafe(size) {
            assertSize(size)
            return createBuffer(size < 0 ? 0 : checked(size) | 0)
        }

        /**
         * Equivalent to Buffer(num), by default creates a non-zero-filled Buffer instance.
         * */
        Buffer.allocUnsafe = function (size) {
            return allocUnsafe(size)
        }
        /**
         * Equivalent to SlowBuffer(num), by default creates a non-zero-filled Buffer instance.
         */
        Buffer.allocUnsafeSlow = function (size) {
            return allocUnsafe(size)
        }

        function fromString(string, encoding) {
            if (typeof encoding !== 'string' || encoding === '') {
                encoding = 'utf8'
            }

            if (!Buffer.isEncoding(encoding)) {
                throw new TypeError('Unknown encoding: ' + encoding)
            }

            var length = byteLength(string, encoding) | 0
            var buf = createBuffer(length)

            var actual = buf.write(string, encoding)

            if (actual !== length) {
                // Writing a hex string, for example, that contains invalid characters will
                // cause everything after the first invalid character to be ignored. (e.g.
                // 'abxxcd' will be treated as 'ab')
                buf = buf.slice(0, actual)
            }

            return buf
        }

        function fromArrayLike(array) {
            var length = array.length < 0 ? 0 : checked(array.length) | 0
            var buf = createBuffer(length)
            for (var i = 0; i < length; i += 1) {
                buf[i] = array[i] & 255
            }
            return buf
        }

        function fromArrayBuffer(array, byteOffset, length) {
            if (byteOffset < 0 || array.byteLength < byteOffset) {
                throw new RangeError('"offset" is outside of buffer bounds')
            }

            if (array.byteLength < byteOffset + (length || 0)) {
                throw new RangeError('"length" is outside of buffer bounds')
            }

            var buf
            if (byteOffset === undefined && length === undefined) {
                buf = new Uint8Array(array)
            } else if (length === undefined) {
                buf = new Uint8Array(array, byteOffset)
            } else {
                buf = new Uint8Array(array, byteOffset, length)
            }

            // Return an augmented `Uint8Array` instance
            buf.__proto__ = Buffer.prototype
            return buf
        }

        function fromObject(obj) {
            if (Buffer.isBuffer(obj)) {
                var len = checked(obj.length) | 0
                var buf = createBuffer(len)

                if (buf.length === 0) {
                    return buf
                }

                obj.copy(buf, 0, 0, len)
                return buf
            }

            if (obj) {
                if (ArrayBuffer.isView(obj) || 'length' in obj) {
                    if (typeof obj.length !== 'number' || numberIsNaN(obj.length)) {
                        return createBuffer(0)
                    }
                    return fromArrayLike(obj)
                }

                if (obj.type === 'Buffer' && Array.isArray(obj.data)) {
                    return fromArrayLike(obj.data)
                }
            }

            throw new TypeError('The first argument must be one of type string, Buffer, ArrayBuffer, Array, or Array-like Object.')
        }

        function checked(length) {
            // Note: cannot use `length < K_MAX_LENGTH` here because that fails when
            // length is NaN (which is otherwise coerced to zero.)
            if (length >= K_MAX_LENGTH) {
                throw new RangeError('Attempt to allocate Buffer larger than maximum ' +
                    'size: 0x' + K_MAX_LENGTH.toString(16) + ' bytes')
            }
            return length | 0
        }

        function SlowBuffer(length) {
            if (+length != length) { // eslint-disable-line eqeqeq
                length = 0
            }
            return Buffer.alloc(+length)
        }

        Buffer.isBuffer = function isBuffer(b) {
            return b != null && b._isBuffer === true
        }

        Buffer.compare = function compare(a, b) {
            if (!Buffer.isBuffer(a) || !Buffer.isBuffer(b)) {
                throw new TypeError('Arguments must be Buffers')
            }

            if (a === b) return 0

            var x = a.length
            var y = b.length

            for (var i = 0, len = Math.min(x, y); i < len; ++i) {
                if (a[i] !== b[i]) {
                    x = a[i]
                    y = b[i]
                    break
                }
            }

            if (x < y) return -1
            if (y < x) return 1
            return 0
        }

        Buffer.isEncoding = function isEncoding(encoding) {
            switch (String(encoding).toLowerCase()) {
                case 'hex':
                case 'utf8':
                case 'utf-8':
                case 'ascii':
                case 'latin1':
                case 'binary':
                case 'base64':
                case 'ucs2':
                case 'ucs-2':
                case 'utf16le':
                case 'utf-16le':
                    return true
                default:
                    return false
            }
        }

        Buffer.concat = function concat(list, length) {
            if (!Array.isArray(list)) {
                throw new TypeError('"list" argument must be an Array of Buffers')
            }

            if (list.length === 0) {
                return Buffer.alloc(0)
            }

            var i
            if (length === undefined) {
                length = 0
                for (i = 0; i < list.length; ++i) {
                    length += list[i].length
                }
            }

            var buffer = Buffer.allocUnsafe(length)
            var pos = 0
            for (i = 0; i < list.length; ++i) {
                var buf = list[i]
                if (ArrayBuffer.isView(buf)) {
                    buf = Buffer.from(buf)
                }
                if (!Buffer.isBuffer(buf)) {
                    throw new TypeError('"list" argument must be an Array of Buffers')
                }
                buf.copy(buffer, pos)
                pos += buf.length
            }
            return buffer
        }

        function byteLength(string, encoding) {
            if (Buffer.isBuffer(string)) {
                return string.length
            }
            if (ArrayBuffer.isView(string) || isArrayBuffer(string)) {
                return string.byteLength
            }
            if (typeof string !== 'string') {
                string = '' + string
            }

            var len = string.length
            if (len === 0) return 0

            // Use a for loop to avoid recursion
            var loweredCase = false
            for (; ;) {
                switch (encoding) {
                    case 'ascii':
                    case 'latin1':
                    case 'binary':
                        return len
                    case 'utf8':
                    case 'utf-8':
                    case undefined:
                        return utf8ToBytes(string).length
                    case 'ucs2':
                    case 'ucs-2':
                    case 'utf16le':
                    case 'utf-16le':
                        return len * 2
                    case 'hex':
                        return len >>> 1
                    case 'base64':
                        return base64ToBytes(string).length
                    default:
                        if (loweredCase) return utf8ToBytes(string).length // assume utf8
                        encoding = ('' + encoding).toLowerCase()
                        loweredCase = true
                }
            }
        }

        Buffer.byteLength = byteLength

        function slowToString(encoding, start, end) {
            var loweredCase = false

            // No need to verify that "this.length <= MAX_UINT32" since it's a read-only
            // property of a typed array.

            // This behaves neither like String nor Uint8Array in that we set start/end
            // to their upper/lower bounds if the value passed is out of range.
            // undefined is handled specially as per ECMA-262 6th Edition,
            // Section 13.3.3.7 Runtime Semantics: KeyedBindingInitialization.
            if (start === undefined || start < 0) {
                start = 0
            }
            // Return early if start > this.length. Done here to prevent potential uint32
            // coercion fail below.
            if (start > this.length) {
                return ''
            }

            if (end === undefined || end > this.length) {
                end = this.length
            }

            if (end <= 0) {
                return ''
            }

            // Force coersion to uint32. This will also coerce falsey/NaN values to 0.
            end >>>= 0
            start >>>= 0

            if (end <= start) {
                return ''
            }

            if (!encoding) encoding = 'utf8'

            while (true) {
                switch (encoding) {
                    case 'hex':
                        return hexSlice(this, start, end)

                    case 'utf8':
                    case 'utf-8':
                        return utf8Slice(this, start, end)

                    case 'ascii':
                        return asciiSlice(this, start, end)

                    case 'latin1':
                    case 'binary':
                        return latin1Slice(this, start, end)

                    case 'base64':
                        return base64Slice(this, start, end)

                    case 'ucs2':
                    case 'ucs-2':
                    case 'utf16le':
                    case 'utf-16le':
                        return utf16leSlice(this, start, end)

                    default:
                        if (loweredCase) throw new TypeError('Unknown encoding: ' + encoding)
                        encoding = (encoding + '').toLowerCase()
                        loweredCase = true
                }
            }
        }

// This property is used by `Buffer.isBuffer` (and the `is-buffer` npm package)
// to detect a Buffer instance. It's not possible to use `instanceof Buffer`
// reliably in a browserify context because there could be multiple different
// copies of the 'buffer' package in use. This method works even for Buffer
// instances that were created from another copy of the `buffer` package.
// See: https://github.com/feross/buffer/issues/154
        Buffer.prototype._isBuffer = true

        function swap(b, n, m) {
            var i = b[n]
            b[n] = b[m]
            b[m] = i
        }

        Buffer.prototype.swap16 = function swap16() {
            var len = this.length
            if (len % 2 !== 0) {
                throw new RangeError('Buffer size must be a multiple of 16-bits')
            }
            for (var i = 0; i < len; i += 2) {
                swap(this, i, i + 1)
            }
            return this
        }

        Buffer.prototype.swap32 = function swap32() {
            var len = this.length
            if (len % 4 !== 0) {
                throw new RangeError('Buffer size must be a multiple of 32-bits')
            }
            for (var i = 0; i < len; i += 4) {
                swap(this, i, i + 3)
                swap(this, i + 1, i + 2)
            }
            return this
        }

        Buffer.prototype.swap64 = function swap64() {
            var len = this.length
            if (len % 8 !== 0) {
                throw new RangeError('Buffer size must be a multiple of 64-bits')
            }
            for (var i = 0; i < len; i += 8) {
                swap(this, i, i + 7)
                swap(this, i + 1, i + 6)
                swap(this, i + 2, i + 5)
                swap(this, i + 3, i + 4)
            }
            return this
        }

        Buffer.prototype.toString = function toString() {
            var length = this.length
            if (length === 0) return ''
            if (arguments.length === 0) return utf8Slice(this, 0, length)
            return slowToString.apply(this, arguments)
        }

        Buffer.prototype.toLocaleString = Buffer.prototype.toString

        Buffer.prototype.equals = function equals(b) {
            if (!Buffer.isBuffer(b)) throw new TypeError('Argument must be a Buffer')
            if (this === b) return true
            return Buffer.compare(this, b) === 0
        }

        Buffer.prototype.inspect = function inspect() {
            var str = ''
            var max = exports.INSPECT_MAX_BYTES
            if (this.length > 0) {
                str = this.toString('hex', 0, max).match(/.{2}/g).join(' ')
                if (this.length > max) str += ' ... '
            }
            return '<Buffer ' + str + '>'
        }

        Buffer.prototype.compare = function compare(target, start, end, thisStart, thisEnd) {
            if (!Buffer.isBuffer(target)) {
                throw new TypeError('Argument must be a Buffer')
            }

            if (start === undefined) {
                start = 0
            }
            if (end === undefined) {
                end = target ? target.length : 0
            }
            if (thisStart === undefined) {
                thisStart = 0
            }
            if (thisEnd === undefined) {
                thisEnd = this.length
            }

            if (start < 0 || end > target.length || thisStart < 0 || thisEnd > this.length) {
                throw new RangeError('out of range index')
            }

            if (thisStart >= thisEnd && start >= end) {
                return 0
            }
            if (thisStart >= thisEnd) {
                return -1
            }
            if (start >= end) {
                return 1
            }

            start >>>= 0
            end >>>= 0
            thisStart >>>= 0
            thisEnd >>>= 0

            if (this === target) return 0

            var x = thisEnd - thisStart
            var y = end - start
            var len = Math.min(x, y)

            var thisCopy = this.slice(thisStart, thisEnd)
            var targetCopy = target.slice(start, end)

            for (var i = 0; i < len; ++i) {
                if (thisCopy[i] !== targetCopy[i]) {
                    x = thisCopy[i]
                    y = targetCopy[i]
                    break
                }
            }

            if (x < y) return -1
            if (y < x) return 1
            return 0
        }

// Finds either the first index of `val` in `buffer` at offset >= `byteOffset`,
// OR the last index of `val` in `buffer` at offset <= `byteOffset`.
//
// Arguments:
// - buffer - a Buffer to search
// - val - a string, Buffer, or number
// - byteOffset - an index into `buffer`; will be clamped to an int32
// - encoding - an optional encoding, relevant is val is a string
// - dir - true for indexOf, false for lastIndexOf
        function bidirectionalIndexOf(buffer, val, byteOffset, encoding, dir) {
            // Empty buffer means no match
            if (buffer.length === 0) return -1

            // Normalize byteOffset
            if (typeof byteOffset === 'string') {
                encoding = byteOffset
                byteOffset = 0
            } else if (byteOffset > 0x7fffffff) {
                byteOffset = 0x7fffffff
            } else if (byteOffset < -0x80000000) {
                byteOffset = -0x80000000
            }
            byteOffset = +byteOffset  // Coerce to Number.
            if (numberIsNaN(byteOffset)) {
                // byteOffset: it it's undefined, null, NaN, "foo", etc, search whole buffer
                byteOffset = dir ? 0 : (buffer.length - 1)
            }

            // Normalize byteOffset: negative offsets start from the end of the buffer
            if (byteOffset < 0) byteOffset = buffer.length + byteOffset
            if (byteOffset >= buffer.length) {
                if (dir) return -1
                else byteOffset = buffer.length - 1
            } else if (byteOffset < 0) {
                if (dir) byteOffset = 0
                else return -1
            }

            // Normalize val
            if (typeof val === 'string') {
                val = Buffer.from(val, encoding)
            }

            // Finally, search either indexOf (if dir is true) or lastIndexOf
            if (Buffer.isBuffer(val)) {
                // Special case: looking for empty string/buffer always fails
                if (val.length === 0) {
                    return -1
                }
                return arrayIndexOf(buffer, val, byteOffset, encoding, dir)
            } else if (typeof val === 'number') {
                val = val & 0xFF // Search for a byte value [0-255]
                if (typeof Uint8Array.prototype.indexOf === 'function') {
                    if (dir) {
                        return Uint8Array.prototype.indexOf.call(buffer, val, byteOffset)
                    } else {
                        return Uint8Array.prototype.lastIndexOf.call(buffer, val, byteOffset)
                    }
                }
                return arrayIndexOf(buffer, [val], byteOffset, encoding, dir)
            }

            throw new TypeError('val must be string, number or Buffer')
        }

        function arrayIndexOf(arr, val, byteOffset, encoding, dir) {
            var indexSize = 1
            var arrLength = arr.length
            var valLength = val.length

            if (encoding !== undefined) {
                encoding = String(encoding).toLowerCase()
                if (encoding === 'ucs2' || encoding === 'ucs-2' ||
                    encoding === 'utf16le' || encoding === 'utf-16le') {
                    if (arr.length < 2 || val.length < 2) {
                        return -1
                    }
                    indexSize = 2
                    arrLength /= 2
                    valLength /= 2
                    byteOffset /= 2
                }
            }

            function read(buf, i) {
                if (indexSize === 1) {
                    return buf[i]
                } else {
                    return buf.readUInt16BE(i * indexSize)
                }
            }

            var i
            if (dir) {
                var foundIndex = -1
                for (i = byteOffset; i < arrLength; i++) {
                    if (read(arr, i) === read(val, foundIndex === -1 ? 0 : i - foundIndex)) {
                        if (foundIndex === -1) foundIndex = i
                        if (i - foundIndex + 1 === valLength) return foundIndex * indexSize
                    } else {
                        if (foundIndex !== -1) i -= i - foundIndex
                        foundIndex = -1
                    }
                }
            } else {
                if (byteOffset + valLength > arrLength) byteOffset = arrLength - valLength
                for (i = byteOffset; i >= 0; i--) {
                    var found = true
                    for (var j = 0; j < valLength; j++) {
                        if (read(arr, i + j) !== read(val, j)) {
                            found = false
                            break
                        }
                    }
                    if (found) return i
                }
            }

            return -1
        }

        Buffer.prototype.includes = function includes(val, byteOffset, encoding) {
            return this.indexOf(val, byteOffset, encoding) !== -1
        }

        Buffer.prototype.indexOf = function indexOf(val, byteOffset, encoding) {
            return bidirectionalIndexOf(this, val, byteOffset, encoding, true)
        }

        Buffer.prototype.lastIndexOf = function lastIndexOf(val, byteOffset, encoding) {
            return bidirectionalIndexOf(this, val, byteOffset, encoding, false)
        }

        function hexWrite(buf, string, offset, length) {
            offset = Number(offset) || 0
            var remaining = buf.length - offset
            if (!length) {
                length = remaining
            } else {
                length = Number(length)
                if (length > remaining) {
                    length = remaining
                }
            }

            var strLen = string.length

            if (length > strLen / 2) {
                length = strLen / 2
            }
            for (var i = 0; i < length; ++i) {
                var parsed = parseInt(string.substr(i * 2, 2), 16)
                if (numberIsNaN(parsed)) return i
                buf[offset + i] = parsed
            }
            return i
        }

        function utf8Write(buf, string, offset, length) {
            return blitBuffer(utf8ToBytes(string, buf.length - offset), buf, offset, length)
        }

        function asciiWrite(buf, string, offset, length) {
            return blitBuffer(asciiToBytes(string), buf, offset, length)
        }

        function latin1Write(buf, string, offset, length) {
            return asciiWrite(buf, string, offset, length)
        }

        function base64Write(buf, string, offset, length) {
            return blitBuffer(base64ToBytes(string), buf, offset, length)
        }

        function ucs2Write(buf, string, offset, length) {
            return blitBuffer(utf16leToBytes(string, buf.length - offset), buf, offset, length)
        }

        Buffer.prototype.write = function write(string, offset, length, encoding) {
            // Buffer#write(string)
            if (offset === undefined) {
                encoding = 'utf8'
                length = this.length
                offset = 0
                // Buffer#write(string, encoding)
            } else if (length === undefined && typeof offset === 'string') {
                encoding = offset
                length = this.length
                offset = 0
                // Buffer#write(string, offset[, length][, encoding])
            } else if (isFinite(offset)) {
                offset = offset >>> 0
                if (isFinite(length)) {
                    length = length >>> 0
                    if (encoding === undefined) encoding = 'utf8'
                } else {
                    encoding = length
                    length = undefined
                }
            } else {
                throw new Error(
                    'Buffer.write(string, encoding, offset[, length]) is no longer supported'
                )
            }

            var remaining = this.length - offset
            if (length === undefined || length > remaining) length = remaining

            if ((string.length > 0 && (length < 0 || offset < 0)) || offset > this.length) {
                throw new RangeError('Attempt to write outside buffer bounds')
            }

            if (!encoding) encoding = 'utf8'

            var loweredCase = false
            for (; ;) {
                switch (encoding) {
                    case 'hex':
                        return hexWrite(this, string, offset, length)

                    case 'utf8':
                    case 'utf-8':
                        return utf8Write(this, string, offset, length)

                    case 'ascii':
                        return asciiWrite(this, string, offset, length)

                    case 'latin1':
                    case 'binary':
                        return latin1Write(this, string, offset, length)

                    case 'base64':
                        // Warning: maxLength not taken into account in base64Write
                        return base64Write(this, string, offset, length)

                    case 'ucs2':
                    case 'ucs-2':
                    case 'utf16le':
                    case 'utf-16le':
                        return ucs2Write(this, string, offset, length)

                    default:
                        if (loweredCase) throw new TypeError('Unknown encoding: ' + encoding)
                        encoding = ('' + encoding).toLowerCase()
                        loweredCase = true
                }
            }
        }

        Buffer.prototype.toJSON = function toJSON() {
            return {
                type: 'Buffer',
                data: Array.prototype.slice.call(this._arr || this, 0)
            }
        }

        function base64Slice(buf, start, end) {
            if (start === 0 && end === buf.length) {
                return base64.fromByteArray(buf)
            } else {
                return base64.fromByteArray(buf.slice(start, end))
            }
        }

        function utf8Slice(buf, start, end) {
            end = Math.min(buf.length, end)
            var res = []

            var i = start
            while (i < end) {
                var firstByte = buf[i]
                var codePoint = null
                var bytesPerSequence = (firstByte > 0xEF) ? 4
                    : (firstByte > 0xDF) ? 3
                        : (firstByte > 0xBF) ? 2
                            : 1

                if (i + bytesPerSequence <= end) {
                    var secondByte, thirdByte, fourthByte, tempCodePoint

                    switch (bytesPerSequence) {
                        case 1:
                            if (firstByte < 0x80) {
                                codePoint = firstByte
                            }
                            break
                        case 2:
                            secondByte = buf[i + 1]
                            if ((secondByte & 0xC0) === 0x80) {
                                tempCodePoint = (firstByte & 0x1F) << 0x6 | (secondByte & 0x3F)
                                if (tempCodePoint > 0x7F) {
                                    codePoint = tempCodePoint
                                }
                            }
                            break
                        case 3:
                            secondByte = buf[i + 1]
                            thirdByte = buf[i + 2]
                            if ((secondByte & 0xC0) === 0x80 && (thirdByte & 0xC0) === 0x80) {
                                tempCodePoint = (firstByte & 0xF) << 0xC | (secondByte & 0x3F) << 0x6 | (thirdByte & 0x3F)
                                if (tempCodePoint > 0x7FF && (tempCodePoint < 0xD800 || tempCodePoint > 0xDFFF)) {
                                    codePoint = tempCodePoint
                                }
                            }
                            break
                        case 4:
                            secondByte = buf[i + 1]
                            thirdByte = buf[i + 2]
                            fourthByte = buf[i + 3]
                            if ((secondByte & 0xC0) === 0x80 && (thirdByte & 0xC0) === 0x80 && (fourthByte & 0xC0) === 0x80) {
                                tempCodePoint = (firstByte & 0xF) << 0x12 | (secondByte & 0x3F) << 0xC | (thirdByte & 0x3F) << 0x6 | (fourthByte & 0x3F)
                                if (tempCodePoint > 0xFFFF && tempCodePoint < 0x110000) {
                                    codePoint = tempCodePoint
                                }
                            }
                    }
                }

                if (codePoint === null) {
                    // we did not generate a valid codePoint so insert a
                    // replacement char (U+FFFD) and advance only 1 byte
                    codePoint = 0xFFFD
                    bytesPerSequence = 1
                } else if (codePoint > 0xFFFF) {
                    // encode to utf16 (surrogate pair dance)
                    codePoint -= 0x10000
                    res.push(codePoint >>> 10 & 0x3FF | 0xD800)
                    codePoint = 0xDC00 | codePoint & 0x3FF
                }

                res.push(codePoint)
                i += bytesPerSequence
            }

            return decodeCodePointsArray(res)
        }

// Based on http://stackoverflow.com/a/22747272/680742, the browser with
// the lowest limit is Chrome, with 0x10000 args.
// We go 1 magnitude less, for safety
        var MAX_ARGUMENTS_LENGTH = 0x1000

        function decodeCodePointsArray(codePoints) {
            var len = codePoints.length
            if (len <= MAX_ARGUMENTS_LENGTH) {
                return String.fromCharCode.apply(String, codePoints) // avoid extra slice()
            }

            // Decode in chunks to avoid "call stack size exceeded".
            var res = ''
            var i = 0
            while (i < len) {
                res += String.fromCharCode.apply(
                    String,
                    codePoints.slice(i, i += MAX_ARGUMENTS_LENGTH)
                )
            }
            return res
        }

        function asciiSlice(buf, start, end) {
            var ret = ''
            end = Math.min(buf.length, end)

            for (var i = start; i < end; ++i) {
                ret += String.fromCharCode(buf[i] & 0x7F)
            }
            return ret
        }

        function latin1Slice(buf, start, end) {
            var ret = ''
            end = Math.min(buf.length, end)

            for (var i = start; i < end; ++i) {
                ret += String.fromCharCode(buf[i])
            }
            return ret
        }

        function hexSlice(buf, start, end) {
            var len = buf.length

            if (!start || start < 0) start = 0
            if (!end || end < 0 || end > len) end = len

            var out = ''
            for (var i = start; i < end; ++i) {
                out += toHex(buf[i])
            }
            return out
        }

        function utf16leSlice(buf, start, end) {
            var bytes = buf.slice(start, end)
            var res = ''
            for (var i = 0; i < bytes.length; i += 2) {
                res += String.fromCharCode(bytes[i] + (bytes[i + 1] * 256))
            }
            return res
        }

        Buffer.prototype.slice = function slice(start, end) {
            var len = this.length
            start = ~~start
            end = end === undefined ? len : ~~end

            if (start < 0) {
                start += len
                if (start < 0) start = 0
            } else if (start > len) {
                start = len
            }

            if (end < 0) {
                end += len
                if (end < 0) end = 0
            } else if (end > len) {
                end = len
            }

            if (end < start) end = start

            var newBuf = this.subarray(start, end)
            // Return an augmented `Uint8Array` instance
            newBuf.__proto__ = Buffer.prototype
            return newBuf
        }

        /*
 * Need to make sure that buffer isn't trying to write out of bounds.
 */
        function checkOffset(offset, ext, length) {
            if ((offset % 1) !== 0 || offset < 0) throw new RangeError('offset is not uint')
            if (offset + ext > length) throw new RangeError('Trying to access beyond buffer length')
        }

        Buffer.prototype.readUIntLE = function readUIntLE(offset, byteLength, noAssert) {
            offset = offset >>> 0
            byteLength = byteLength >>> 0
            if (!noAssert) checkOffset(offset, byteLength, this.length)

            var val = this[offset]
            var mul = 1
            var i = 0
            while (++i < byteLength && (mul *= 0x100)) {
                val += this[offset + i] * mul
            }

            return val
        }

        Buffer.prototype.readUIntBE = function readUIntBE(offset, byteLength, noAssert) {
            offset = offset >>> 0
            byteLength = byteLength >>> 0
            if (!noAssert) {
                checkOffset(offset, byteLength, this.length)
            }

            var val = this[offset + --byteLength]
            var mul = 1
            while (byteLength > 0 && (mul *= 0x100)) {
                val += this[offset + --byteLength] * mul
            }

            return val
        }

        Buffer.prototype.readUInt8 = function readUInt8(offset, noAssert) {
            offset = offset >>> 0
            if (!noAssert) checkOffset(offset, 1, this.length)
            return this[offset]
        }

        Buffer.prototype.readUInt16LE = function readUInt16LE(offset, noAssert) {
            offset = offset >>> 0
            if (!noAssert) checkOffset(offset, 2, this.length)
            return this[offset] | (this[offset + 1] << 8)
        }

        Buffer.prototype.readUInt16BE = function readUInt16BE(offset, noAssert) {
            offset = offset >>> 0
            if (!noAssert) checkOffset(offset, 2, this.length)
            return (this[offset] << 8) | this[offset + 1]
        }

        Buffer.prototype.readUInt32LE = function readUInt32LE(offset, noAssert) {
            offset = offset >>> 0
            if (!noAssert) checkOffset(offset, 4, this.length)

            return ((this[offset]) |
                (this[offset + 1] << 8) |
                (this[offset + 2] << 16)) +
                (this[offset + 3] * 0x1000000)
        }

        Buffer.prototype.readUInt32BE = function readUInt32BE(offset, noAssert) {
            offset = offset >>> 0
            if (!noAssert) checkOffset(offset, 4, this.length)

            return (this[offset] * 0x1000000) +
                ((this[offset + 1] << 16) |
                    (this[offset + 2] << 8) |
                    this[offset + 3])
        }

        Buffer.prototype.readIntLE = function readIntLE(offset, byteLength, noAssert) {
            offset = offset >>> 0
            byteLength = byteLength >>> 0
            if (!noAssert) checkOffset(offset, byteLength, this.length)

            var val = this[offset]
            var mul = 1
            var i = 0
            while (++i < byteLength && (mul *= 0x100)) {
                val += this[offset + i] * mul
            }
            mul *= 0x80

            if (val >= mul) val -= Math.pow(2, 8 * byteLength)

            return val
        }

        Buffer.prototype.readIntBE = function readIntBE(offset, byteLength, noAssert) {
            offset = offset >>> 0
            byteLength = byteLength >>> 0
            if (!noAssert) checkOffset(offset, byteLength, this.length)

            var i = byteLength
            var mul = 1
            var val = this[offset + --i]
            while (i > 0 && (mul *= 0x100)) {
                val += this[offset + --i] * mul
            }
            mul *= 0x80

            if (val >= mul) val -= Math.pow(2, 8 * byteLength)

            return val
        }

        Buffer.prototype.readInt8 = function readInt8(offset, noAssert) {
            offset = offset >>> 0
            if (!noAssert) checkOffset(offset, 1, this.length)
            if (!(this[offset] & 0x80)) return (this[offset])
            return ((0xff - this[offset] + 1) * -1)
        }

        Buffer.prototype.readInt16LE = function readInt16LE(offset, noAssert) {
            offset = offset >>> 0
            if (!noAssert) checkOffset(offset, 2, this.length)
            var val = this[offset] | (this[offset + 1] << 8)
            return (val & 0x8000) ? val | 0xFFFF0000 : val
        }

        Buffer.prototype.readInt16BE = function readInt16BE(offset, noAssert) {
            offset = offset >>> 0
            if (!noAssert) checkOffset(offset, 2, this.length)
            var val = this[offset + 1] | (this[offset] << 8)
            return (val & 0x8000) ? val | 0xFFFF0000 : val
        }

        Buffer.prototype.readInt32LE = function readInt32LE(offset, noAssert) {
            offset = offset >>> 0
            if (!noAssert) checkOffset(offset, 4, this.length)

            return (this[offset]) |
                (this[offset + 1] << 8) |
                (this[offset + 2] << 16) |
                (this[offset + 3] << 24)
        }

        Buffer.prototype.readInt32BE = function readInt32BE(offset, noAssert) {
            offset = offset >>> 0
            if (!noAssert) checkOffset(offset, 4, this.length)

            return (this[offset] << 24) |
                (this[offset + 1] << 16) |
                (this[offset + 2] << 8) |
                (this[offset + 3])
        }

        Buffer.prototype.readFloatLE = function readFloatLE(offset, noAssert) {
            offset = offset >>> 0
            if (!noAssert) checkOffset(offset, 4, this.length)
            return ieee754.read(this, offset, true, 23, 4)
        }

        Buffer.prototype.readFloatBE = function readFloatBE(offset, noAssert) {
            offset = offset >>> 0
            if (!noAssert) checkOffset(offset, 4, this.length)
            return ieee754.read(this, offset, false, 23, 4)
        }

        Buffer.prototype.readDoubleLE = function readDoubleLE(offset, noAssert) {
            offset = offset >>> 0
            if (!noAssert) checkOffset(offset, 8, this.length)
            return ieee754.read(this, offset, true, 52, 8)
        }

        Buffer.prototype.readDoubleBE = function readDoubleBE(offset, noAssert) {
            offset = offset >>> 0
            if (!noAssert) checkOffset(offset, 8, this.length)
            return ieee754.read(this, offset, false, 52, 8)
        }

        function checkInt(buf, value, offset, ext, max, min) {
            if (!Buffer.isBuffer(buf)) throw new TypeError('"buffer" argument must be a Buffer instance')
            if (value > max || value < min) throw new RangeError('"value" argument is out of bounds')
            if (offset + ext > buf.length) throw new RangeError('Index out of range')
        }

        Buffer.prototype.writeUIntLE = function writeUIntLE(value, offset, byteLength, noAssert) {
            value = +value
            offset = offset >>> 0
            byteLength = byteLength >>> 0
            if (!noAssert) {
                var maxBytes = Math.pow(2, 8 * byteLength) - 1
                checkInt(this, value, offset, byteLength, maxBytes, 0)
            }

            var mul = 1
            var i = 0
            this[offset] = value & 0xFF
            while (++i < byteLength && (mul *= 0x100)) {
                this[offset + i] = (value / mul) & 0xFF
            }

            return offset + byteLength
        }

        Buffer.prototype.writeUIntBE = function writeUIntBE(value, offset, byteLength, noAssert) {
            value = +value
            offset = offset >>> 0
            byteLength = byteLength >>> 0
            if (!noAssert) {
                var maxBytes = Math.pow(2, 8 * byteLength) - 1
                checkInt(this, value, offset, byteLength, maxBytes, 0)
            }

            var i = byteLength - 1
            var mul = 1
            this[offset + i] = value & 0xFF
            while (--i >= 0 && (mul *= 0x100)) {
                this[offset + i] = (value / mul) & 0xFF
            }

            return offset + byteLength
        }

        Buffer.prototype.writeUInt8 = function writeUInt8(value, offset, noAssert) {
            value = +value
            offset = offset >>> 0
            if (!noAssert) checkInt(this, value, offset, 1, 0xff, 0)
            this[offset] = (value & 0xff)
            return offset + 1
        }

        Buffer.prototype.writeUInt16LE = function writeUInt16LE(value, offset, noAssert) {
            value = +value
            offset = offset >>> 0
            if (!noAssert) checkInt(this, value, offset, 2, 0xffff, 0)
            this[offset] = (value & 0xff)
            this[offset + 1] = (value >>> 8)
            return offset + 2
        }

        Buffer.prototype.writeUInt16BE = function writeUInt16BE(value, offset, noAssert) {
            value = +value
            offset = offset >>> 0
            if (!noAssert) checkInt(this, value, offset, 2, 0xffff, 0)
            this[offset] = (value >>> 8)
            this[offset + 1] = (value & 0xff)
            return offset + 2
        }

        Buffer.prototype.writeUInt32LE = function writeUInt32LE(value, offset, noAssert) {
            value = +value
            offset = offset >>> 0
            if (!noAssert) checkInt(this, value, offset, 4, 0xffffffff, 0)
            this[offset + 3] = (value >>> 24)
            this[offset + 2] = (value >>> 16)
            this[offset + 1] = (value >>> 8)
            this[offset] = (value & 0xff)
            return offset + 4
        }

        Buffer.prototype.writeUInt32BE = function writeUInt32BE(value, offset, noAssert) {
            value = +value
            offset = offset >>> 0
            if (!noAssert) checkInt(this, value, offset, 4, 0xffffffff, 0)
            this[offset] = (value >>> 24)
            this[offset + 1] = (value >>> 16)
            this[offset + 2] = (value >>> 8)
            this[offset + 3] = (value & 0xff)
            return offset + 4
        }

        Buffer.prototype.writeIntLE = function writeIntLE(value, offset, byteLength, noAssert) {
            value = +value
            offset = offset >>> 0
            if (!noAssert) {
                var limit = Math.pow(2, (8 * byteLength) - 1)

                checkInt(this, value, offset, byteLength, limit - 1, -limit)
            }

            var i = 0
            var mul = 1
            var sub = 0
            this[offset] = value & 0xFF
            while (++i < byteLength && (mul *= 0x100)) {
                if (value < 0 && sub === 0 && this[offset + i - 1] !== 0) {
                    sub = 1
                }
                this[offset + i] = ((value / mul) >> 0) - sub & 0xFF
            }

            return offset + byteLength
        }

        Buffer.prototype.writeIntBE = function writeIntBE(value, offset, byteLength, noAssert) {
            value = +value
            offset = offset >>> 0
            if (!noAssert) {
                var limit = Math.pow(2, (8 * byteLength) - 1)

                checkInt(this, value, offset, byteLength, limit - 1, -limit)
            }

            var i = byteLength - 1
            var mul = 1
            var sub = 0
            this[offset + i] = value & 0xFF
            while (--i >= 0 && (mul *= 0x100)) {
                if (value < 0 && sub === 0 && this[offset + i + 1] !== 0) {
                    sub = 1
                }
                this[offset + i] = ((value / mul) >> 0) - sub & 0xFF
            }

            return offset + byteLength
        }

        Buffer.prototype.writeInt8 = function writeInt8(value, offset, noAssert) {
            value = +value
            offset = offset >>> 0
            if (!noAssert) checkInt(this, value, offset, 1, 0x7f, -0x80)
            if (value < 0) value = 0xff + value + 1
            this[offset] = (value & 0xff)
            return offset + 1
        }

        Buffer.prototype.writeInt16LE = function writeInt16LE(value, offset, noAssert) {
            value = +value
            offset = offset >>> 0
            if (!noAssert) checkInt(this, value, offset, 2, 0x7fff, -0x8000)
            this[offset] = (value & 0xff)
            this[offset + 1] = (value >>> 8)
            return offset + 2
        }

        Buffer.prototype.writeInt16BE = function writeInt16BE(value, offset, noAssert) {
            value = +value
            offset = offset >>> 0
            if (!noAssert) checkInt(this, value, offset, 2, 0x7fff, -0x8000)
            this[offset] = (value >>> 8)
            this[offset + 1] = (value & 0xff)
            return offset + 2
        }

        Buffer.prototype.writeInt32LE = function writeInt32LE(value, offset, noAssert) {
            value = +value
            offset = offset >>> 0
            if (!noAssert) checkInt(this, value, offset, 4, 0x7fffffff, -0x80000000)
            this[offset] = (value & 0xff)
            this[offset + 1] = (value >>> 8)
            this[offset + 2] = (value >>> 16)
            this[offset + 3] = (value >>> 24)
            return offset + 4
        }

        Buffer.prototype.writeInt32BE = function writeInt32BE(value, offset, noAssert) {
            value = +value
            offset = offset >>> 0
            if (!noAssert) checkInt(this, value, offset, 4, 0x7fffffff, -0x80000000)
            if (value < 0) value = 0xffffffff + value + 1
            this[offset] = (value >>> 24)
            this[offset + 1] = (value >>> 16)
            this[offset + 2] = (value >>> 8)
            this[offset + 3] = (value & 0xff)
            return offset + 4
        }

        function checkIEEE754(buf, value, offset, ext, max, min) {
            if (offset + ext > buf.length) throw new RangeError('Index out of range')
            if (offset < 0) throw new RangeError('Index out of range')
        }

        function writeFloat(buf, value, offset, littleEndian, noAssert) {
            value = +value
            offset = offset >>> 0
            if (!noAssert) {
                checkIEEE754(buf, value, offset, 4, 3.4028234663852886e+38, -3.4028234663852886e+38)
            }
            ieee754.write(buf, value, offset, littleEndian, 23, 4)
            return offset + 4
        }

        Buffer.prototype.writeFloatLE = function writeFloatLE(value, offset, noAssert) {
            return writeFloat(this, value, offset, true, noAssert)
        }

        Buffer.prototype.writeFloatBE = function writeFloatBE(value, offset, noAssert) {
            return writeFloat(this, value, offset, false, noAssert)
        }

        function writeDouble(buf, value, offset, littleEndian, noAssert) {
            value = +value
            offset = offset >>> 0
            if (!noAssert) {
                checkIEEE754(buf, value, offset, 8, 1.7976931348623157E+308, -1.7976931348623157E+308)
            }
            ieee754.write(buf, value, offset, littleEndian, 52, 8)
            return offset + 8
        }

        Buffer.prototype.writeDoubleLE = function writeDoubleLE(value, offset, noAssert) {
            return writeDouble(this, value, offset, true, noAssert)
        }

        Buffer.prototype.writeDoubleBE = function writeDoubleBE(value, offset, noAssert) {
            return writeDouble(this, value, offset, false, noAssert)
        }

// copy(targetBuffer, targetStart=0, sourceStart=0, sourceEnd=buffer.length)
        Buffer.prototype.copy = function copy(target, targetStart, start, end) {
            if (!Buffer.isBuffer(target)) throw new TypeError('argument should be a Buffer')
            if (!start) start = 0
            if (!end && end !== 0) end = this.length
            if (targetStart >= target.length) targetStart = target.length
            if (!targetStart) targetStart = 0
            if (end > 0 && end < start) end = start

            // Copy 0 bytes; we're done
            if (end === start) return 0
            if (target.length === 0 || this.length === 0) return 0

            // Fatal error conditions
            if (targetStart < 0) {
                throw new RangeError('targetStart out of bounds')
            }
            if (start < 0 || start >= this.length) throw new RangeError('Index out of range')
            if (end < 0) throw new RangeError('sourceEnd out of bounds')

            // Are we oob?
            if (end > this.length) end = this.length
            if (target.length - targetStart < end - start) {
                end = target.length - targetStart + start
            }

            var len = end - start

            if (this === target && typeof Uint8Array.prototype.copyWithin === 'function') {
                // Use built-in when available, missing from IE11
                this.copyWithin(targetStart, start, end)
            } else if (this === target && start < targetStart && targetStart < end) {
                // descending copy from end
                for (var i = len - 1; i >= 0; --i) {
                    target[i + targetStart] = this[i + start]
                }
            } else {
                Uint8Array.prototype.set.call(
                    target,
                    this.subarray(start, end),
                    targetStart
                )
            }

            return len
        }

// Usage:
//    buffer.fill(number[, offset[, end]])
//    buffer.fill(buffer[, offset[, end]])
//    buffer.fill(string[, offset[, end]][, encoding])
        Buffer.prototype.fill = function fill(val, start, end, encoding) {
            // Handle string cases:
            if (typeof val === 'string') {
                if (typeof start === 'string') {
                    encoding = start
                    start = 0
                    end = this.length
                } else if (typeof end === 'string') {
                    encoding = end
                    end = this.length
                }
                if (encoding !== undefined && typeof encoding !== 'string') {
                    throw new TypeError('encoding must be a string')
                }
                if (typeof encoding === 'string' && !Buffer.isEncoding(encoding)) {
                    throw new TypeError('Unknown encoding: ' + encoding)
                }
                if (val.length === 1) {
                    var code = val.charCodeAt(0)
                    if ((encoding === 'utf8' && code < 128) ||
                        encoding === 'latin1') {
                        // Fast path: If `val` fits into a single byte, use that numeric value.
                        val = code
                    }
                }
            } else if (typeof val === 'number') {
                val = val & 255
            }

            // Invalid ranges are not set to a default, so can range check early.
            if (start < 0 || this.length < start || this.length < end) {
                throw new RangeError('Out of range index')
            }

            if (end <= start) {
                return this
            }

            start = start >>> 0
            end = end === undefined ? this.length : end >>> 0

            if (!val) val = 0

            var i
            if (typeof val === 'number') {
                for (i = start; i < end; ++i) {
                    this[i] = val
                }
            } else {
                var bytes = Buffer.isBuffer(val)
                    ? val
                    : new Buffer(val, encoding)
                var len = bytes.length
                if (len === 0) {
                    throw new TypeError('The value "' + val +
                        '" is invalid for argument "value"')
                }
                for (i = 0; i < end - start; ++i) {
                    this[i + start] = bytes[i % len]
                }
            }

            return this
        }

// HELPER FUNCTIONS
// ================

        var INVALID_BASE64_RE = /[^+/0-9A-Za-z-_]/g

        function base64clean(str) {
            // Node takes equal signs as end of the Base64 encoding
            str = str.split('=')[0]
            // Node strips out invalid characters like \n and \t from the string, base64-js does not
            str = str.trim().replace(INVALID_BASE64_RE, '')
            // Node converts strings with length < 2 to ''
            if (str.length < 2) return ''
            // Node allows for non-padded base64 strings (missing trailing ===), base64-js does not
            while (str.length % 4 !== 0) {
                str = str + '='
            }
            return str
        }

        function toHex(n) {
            if (n < 16) return '0' + n.toString(16)
            return n.toString(16)
        }

        function utf8ToBytes(string, units) {
            units = units || Infinity
            var codePoint
            var length = string.length
            var leadSurrogate = null
            var bytes = []

            for (var i = 0; i < length; ++i) {
                codePoint = string.charCodeAt(i)

                // is surrogate component
                if (codePoint > 0xD7FF && codePoint < 0xE000) {
                    // last char was a lead
                    if (!leadSurrogate) {
                        // no lead yet
                        if (codePoint > 0xDBFF) {
                            // unexpected trail
                            if ((units -= 3) > -1) bytes.push(0xEF, 0xBF, 0xBD)
                            continue
                        } else if (i + 1 === length) {
                            // unpaired lead
                            if ((units -= 3) > -1) bytes.push(0xEF, 0xBF, 0xBD)
                            continue
                        }

                        // valid lead
                        leadSurrogate = codePoint

                        continue
                    }

                    // 2 leads in a row
                    if (codePoint < 0xDC00) {
                        if ((units -= 3) > -1) bytes.push(0xEF, 0xBF, 0xBD)
                        leadSurrogate = codePoint
                        continue
                    }

                    // valid surrogate pair
                    codePoint = (leadSurrogate - 0xD800 << 10 | codePoint - 0xDC00) + 0x10000
                } else if (leadSurrogate) {
                    // valid bmp char, but last char was a lead
                    if ((units -= 3) > -1) bytes.push(0xEF, 0xBF, 0xBD)
                }

                leadSurrogate = null

                // encode utf8
                if (codePoint < 0x80) {
                    if ((units -= 1) < 0) break
                    bytes.push(codePoint)
                } else if (codePoint < 0x800) {
                    if ((units -= 2) < 0) break
                    bytes.push(
                        codePoint >> 0x6 | 0xC0,
                        codePoint & 0x3F | 0x80
                    )
                } else if (codePoint < 0x10000) {
                    if ((units -= 3) < 0) break
                    bytes.push(
                        codePoint >> 0xC | 0xE0,
                        codePoint >> 0x6 & 0x3F | 0x80,
                        codePoint & 0x3F | 0x80
                    )
                } else if (codePoint < 0x110000) {
                    if ((units -= 4) < 0) break
                    bytes.push(
                        codePoint >> 0x12 | 0xF0,
                        codePoint >> 0xC & 0x3F | 0x80,
                        codePoint >> 0x6 & 0x3F | 0x80,
                        codePoint & 0x3F | 0x80
                    )
                } else {
                    throw new Error('Invalid code point')
                }
            }

            return bytes
        }

        function asciiToBytes(str) {
            var byteArray = []
            for (var i = 0; i < str.length; ++i) {
                // Node's code seems to be doing this and not & 0x7F..
                byteArray.push(str.charCodeAt(i) & 0xFF)
            }
            return byteArray
        }

        function utf16leToBytes(str, units) {
            var c, hi, lo
            var byteArray = []
            for (var i = 0; i < str.length; ++i) {
                if ((units -= 2) < 0) break

                c = str.charCodeAt(i)
                hi = c >> 8
                lo = c % 256
                byteArray.push(lo)
                byteArray.push(hi)
            }

            return byteArray
        }

        function base64ToBytes(str) {
            return base64.toByteArray(base64clean(str))
        }

        function blitBuffer(src, dst, offset, length) {
            for (var i = 0; i < length; ++i) {
                if ((i + offset >= dst.length) || (i >= src.length)) break
                dst[i + offset] = src[i]
            }
            return i
        }

// ArrayBuffers from another context (i.e. an iframe) do not pass the `instanceof` check
// but they should be treated as valid. See: https://github.com/feross/buffer/issues/166
        function isArrayBuffer(obj) {
            return obj instanceof ArrayBuffer ||
                (obj != null && obj.constructor != null && obj.constructor.name === 'ArrayBuffer' &&
                    typeof obj.byteLength === 'number')
        }

        function numberIsNaN(obj) {
            return obj !== obj // eslint-disable-line no-self-compare
        }

    }, {"base64-js": 2, "ieee754": 10}], 9: [function (require, module, exports) {
        "use strict"

        function dupe_array(count, value, i) {
            var c = count[i] | 0
            if (c <= 0) {
                return []
            }
            var result = new Array(c), j
            if (i === count.length - 1) {
                for (j = 0; j < c; ++j) {
                    result[j] = value
                }
            } else {
                for (j = 0; j < c; ++j) {
                    result[j] = dupe_array(count, value, i + 1)
                }
            }
            return result
        }

        function dupe_number(count, value) {
            var result, i
            result = new Array(count)
            for (i = 0; i < count; ++i) {
                result[i] = value
            }
            return result
        }

        function dupe(count, value) {
            if (typeof value === "undefined") {
                value = 0
            }
            switch (typeof count) {
                case "number":
                    if (count > 0) {
                        return dupe_number(count | 0, value)
                    }
                    break
                case "object":
                    if (typeof (count.length) === "number") {
                        return dupe_array(count, value, 0)
                    }
                    break
            }
            return []
        }

        module.exports = dupe
    }, {}], 10: [function (require, module, exports) {
        exports.read = function (buffer, offset, isLE, mLen, nBytes) {
            var e, m
            var eLen = (nBytes * 8) - mLen - 1
            var eMax = (1 << eLen) - 1
            var eBias = eMax >> 1
            var nBits = -7
            var i = isLE ? (nBytes - 1) : 0
            var d = isLE ? -1 : 1
            var s = buffer[offset + i]

            i += d

            e = s & ((1 << (-nBits)) - 1)
            s >>= (-nBits)
            nBits += eLen
            for (; nBits > 0; e = (e * 256) + buffer[offset + i], i += d, nBits -= 8) {
            }

            m = e & ((1 << (-nBits)) - 1)
            e >>= (-nBits)
            nBits += mLen
            for (; nBits > 0; m = (m * 256) + buffer[offset + i], i += d, nBits -= 8) {
            }

            if (e === 0) {
                e = 1 - eBias
            } else if (e === eMax) {
                return m ? NaN : ((s ? -1 : 1) * Infinity)
            } else {
                m = m + Math.pow(2, mLen)
                e = e - eBias
            }
            return (s ? -1 : 1) * m * Math.pow(2, e - mLen)
        }

        exports.write = function (buffer, value, offset, isLE, mLen, nBytes) {
            var e, m, c
            var eLen = (nBytes * 8) - mLen - 1
            var eMax = (1 << eLen) - 1
            var eBias = eMax >> 1
            var rt = (mLen === 23 ? Math.pow(2, -24) - Math.pow(2, -77) : 0)
            var i = isLE ? 0 : (nBytes - 1)
            var d = isLE ? 1 : -1
            var s = value < 0 || (value === 0 && 1 / value < 0) ? 1 : 0

            value = Math.abs(value)

            if (isNaN(value) || value === Infinity) {
                m = isNaN(value) ? 1 : 0
                e = eMax
            } else {
                e = Math.floor(Math.log(value) / Math.LN2)
                if (value * (c = Math.pow(2, -e)) < 1) {
                    e--
                    c *= 2
                }
                if (e + eBias >= 1) {
                    value += rt / c
                } else {
                    value += rt * Math.pow(2, 1 - eBias)
                }
                if (value * c >= 2) {
                    e++
                    c /= 2
                }

                if (e + eBias >= eMax) {
                    m = 0
                    e = eMax
                } else if (e + eBias >= 1) {
                    m = ((value * c) - 1) * Math.pow(2, mLen)
                    e = e + eBias
                } else {
                    m = value * Math.pow(2, eBias - 1) * Math.pow(2, mLen)
                    e = 0
                }
            }

            for (; mLen >= 8; buffer[offset + i] = m & 0xff, i += d, m /= 256, mLen -= 8) {
            }

            e = (e << mLen) | m
            eLen += mLen
            for (; eLen > 0; buffer[offset + i] = e & 0xff, i += d, e /= 256, eLen -= 8) {
            }

            buffer[offset + i - d] |= s * 128
        }

    }, {}], 11: [function (require, module, exports) {
        "use strict"

        var bounds = require("binary-search-bounds")

        var NOT_FOUND = 0
        var SUCCESS = 1
        var EMPTY = 2

        module.exports = createWrapper

        function IntervalTreeNode(mid, left, right, leftPoints, rightPoints) {
            this.mid = mid
            this.left = left
            this.right = right
            this.leftPoints = leftPoints
            this.rightPoints = rightPoints
            this.count = (left ? left.count : 0) + (right ? right.count : 0) + leftPoints.length
        }

        var proto = IntervalTreeNode.prototype

        function copy(a, b) {
            a.mid = b.mid
            a.left = b.left
            a.right = b.right
            a.leftPoints = b.leftPoints
            a.rightPoints = b.rightPoints
            a.count = b.count
        }

        function rebuild(node, intervals) {
            var ntree = createIntervalTree(intervals)
            node.mid = ntree.mid
            node.left = ntree.left
            node.right = ntree.right
            node.leftPoints = ntree.leftPoints
            node.rightPoints = ntree.rightPoints
            node.count = ntree.count
        }

        function rebuildWithInterval(node, interval) {
            var intervals = node.intervals([])
            intervals.push(interval)
            rebuild(node, intervals)
        }

        function rebuildWithoutInterval(node, interval) {
            var intervals = node.intervals([])
            var idx = intervals.indexOf(interval)
            if (idx < 0) {
                return NOT_FOUND
            }
            intervals.splice(idx, 1)
            rebuild(node, intervals)
            return SUCCESS
        }

        proto.intervals = function (result) {
            result.push.apply(result, this.leftPoints)
            if (this.left) {
                this.left.intervals(result)
            }
            if (this.right) {
                this.right.intervals(result)
            }
            return result
        }

        proto.insert = function (interval) {
            var weight = this.count - this.leftPoints.length
            this.count += 1
            if (interval[1] < this.mid) {
                if (this.left) {
                    if (4 * (this.left.count + 1) > 3 * (weight + 1)) {
                        rebuildWithInterval(this, interval)
                    } else {
                        this.left.insert(interval)
                    }
                } else {
                    this.left = createIntervalTree([interval])
                }
            } else if (interval[0] > this.mid) {
                if (this.right) {
                    if (4 * (this.right.count + 1) > 3 * (weight + 1)) {
                        rebuildWithInterval(this, interval)
                    } else {
                        this.right.insert(interval)
                    }
                } else {
                    this.right = createIntervalTree([interval])
                }
            } else {
                var l = bounds.ge(this.leftPoints, interval, compareBegin)
                var r = bounds.ge(this.rightPoints, interval, compareEnd)
                this.leftPoints.splice(l, 0, interval)
                this.rightPoints.splice(r, 0, interval)
            }
        }

        proto.remove = function (interval) {
            var weight = this.count - this.leftPoints
            if (interval[1] < this.mid) {
                if (!this.left) {
                    return NOT_FOUND
                }
                var rw = this.right ? this.right.count : 0
                if (4 * rw > 3 * (weight - 1)) {
                    return rebuildWithoutInterval(this, interval)
                }
                var r = this.left.remove(interval)
                if (r === EMPTY) {
                    this.left = null
                    this.count -= 1
                    return SUCCESS
                } else if (r === SUCCESS) {
                    this.count -= 1
                }
                return r
            } else if (interval[0] > this.mid) {
                if (!this.right) {
                    return NOT_FOUND
                }
                var lw = this.left ? this.left.count : 0
                if (4 * lw > 3 * (weight - 1)) {
                    return rebuildWithoutInterval(this, interval)
                }
                var r = this.right.remove(interval)
                if (r === EMPTY) {
                    this.right = null
                    this.count -= 1
                    return SUCCESS
                } else if (r === SUCCESS) {
                    this.count -= 1
                }
                return r
            } else {
                if (this.count === 1) {
                    if (this.leftPoints[0] === interval) {
                        return EMPTY
                    } else {
                        return NOT_FOUND
                    }
                }
                if (this.leftPoints.length === 1 && this.leftPoints[0] === interval) {
                    if (this.left && this.right) {
                        var p = this
                        var n = this.left
                        while (n.right) {
                            p = n
                            n = n.right
                        }
                        if (p === this) {
                            n.right = this.right
                        } else {
                            var l = this.left
                            var r = this.right
                            p.count -= n.count
                            p.right = n.left
                            n.left = l
                            n.right = r
                        }
                        copy(this, n)
                        this.count = (this.left ? this.left.count : 0) + (this.right ? this.right.count : 0) + this.leftPoints.length
                    } else if (this.left) {
                        copy(this, this.left)
                    } else {
                        copy(this, this.right)
                    }
                    return SUCCESS
                }
                for (var l = bounds.ge(this.leftPoints, interval, compareBegin); l < this.leftPoints.length; ++l) {
                    if (this.leftPoints[l][0] !== interval[0]) {
                        break
                    }
                    if (this.leftPoints[l] === interval) {
                        this.count -= 1
                        this.leftPoints.splice(l, 1)
                        for (var r = bounds.ge(this.rightPoints, interval, compareEnd); r < this.rightPoints.length; ++r) {
                            if (this.rightPoints[r][1] !== interval[1]) {
                                break
                            } else if (this.rightPoints[r] === interval) {
                                this.rightPoints.splice(r, 1)
                                return SUCCESS
                            }
                        }
                    }
                }
                return NOT_FOUND
            }
        }

        function reportLeftRange(arr, hi, cb) {
            for (var i = 0; i < arr.length && arr[i][0] <= hi; ++i) {
                var r = cb(arr[i])
                if (r) {
                    return r
                }
            }
        }

        function reportRightRange(arr, lo, cb) {
            for (var i = arr.length - 1; i >= 0 && arr[i][1] >= lo; --i) {
                var r = cb(arr[i])
                if (r) {
                    return r
                }
            }
        }

        function reportRange(arr, cb) {
            for (var i = 0; i < arr.length; ++i) {
                var r = cb(arr[i])
                if (r) {
                    return r
                }
            }
        }

        proto.queryPoint = function (x, cb) {
            if (x < this.mid) {
                if (this.left) {
                    var r = this.left.queryPoint(x, cb)
                    if (r) {
                        return r
                    }
                }
                return reportLeftRange(this.leftPoints, x, cb)
            } else if (x > this.mid) {
                if (this.right) {
                    var r = this.right.queryPoint(x, cb)
                    if (r) {
                        return r
                    }
                }
                return reportRightRange(this.rightPoints, x, cb)
            } else {
                return reportRange(this.leftPoints, cb)
            }
        }

        proto.queryInterval = function (lo, hi, cb) {
            if (lo < this.mid && this.left) {
                var r = this.left.queryInterval(lo, hi, cb)
                if (r) {
                    return r
                }
            }
            if (hi > this.mid && this.right) {
                var r = this.right.queryInterval(lo, hi, cb)
                if (r) {
                    return r
                }
            }
            if (hi < this.mid) {
                return reportLeftRange(this.leftPoints, hi, cb)
            } else if (lo > this.mid) {
                return reportRightRange(this.rightPoints, lo, cb)
            } else {
                return reportRange(this.leftPoints, cb)
            }
        }

        function compareNumbers(a, b) {
            return a - b
        }

        function compareBegin(a, b) {
            var d = a[0] - b[0]
            if (d) {
                return d
            }
            return a[1] - b[1]
        }

        function compareEnd(a, b) {
            var d = a[1] - b[1]
            if (d) {
                return d
            }
            return a[0] - b[0]
        }

        function createIntervalTree(intervals) {
            if (intervals.length === 0) {
                return null
            }
            var pts = []
            for (var i = 0; i < intervals.length; ++i) {
                pts.push(intervals[i][0], intervals[i][1])
            }
            pts.sort(compareNumbers)

            var mid = pts[pts.length >> 1]

            var leftIntervals = []
            var rightIntervals = []
            var centerIntervals = []
            for (var i = 0; i < intervals.length; ++i) {
                var s = intervals[i]
                if (s[1] < mid) {
                    leftIntervals.push(s)
                } else if (mid < s[0]) {
                    rightIntervals.push(s)
                } else {
                    centerIntervals.push(s)
                }
            }

            //Split center intervals
            var leftPoints = centerIntervals
            var rightPoints = centerIntervals.slice()
            leftPoints.sort(compareBegin)
            rightPoints.sort(compareEnd)

            return new IntervalTreeNode(mid,
                createIntervalTree(leftIntervals),
                createIntervalTree(rightIntervals),
                leftPoints,
                rightPoints)
        }

//User friendly wrapper that makes it possible to support empty trees
        function IntervalTree(root) {
            this.root = root
        }

        var tproto = IntervalTree.prototype

        tproto.insert = function (interval) {
            if (this.root) {
                this.root.insert(interval)
            } else {
                this.root = new IntervalTreeNode(interval[0], null, null, [interval], [interval])
            }
        }

        tproto.remove = function (interval) {
            if (this.root) {
                var r = this.root.remove(interval)
                if (r === EMPTY) {
                    this.root = null
                }
                return r !== NOT_FOUND
            }
            return false
        }

        tproto.queryPoint = function (p, cb) {
            if (this.root) {
                return this.root.queryPoint(p, cb)
            }
        }

        tproto.queryInterval = function (lo, hi, cb) {
            if (lo <= hi && this.root) {
                return this.root.queryInterval(lo, hi, cb)
            }
        }

        Object.defineProperty(tproto, "count", {
            get: function () {
                if (this.root) {
                    return this.root.count
                }
                return 0
            }
        })

        Object.defineProperty(tproto, "intervals", {
            get: function () {
                if (this.root) {
                    return this.root.intervals([])
                }
                return []
            }
        })

        function createWrapper(intervals) {
            if (!intervals || intervals.length === 0) {
                return new IntervalTree(null)
            }
            return new IntervalTree(createIntervalTree(intervals))
        }

    }, {"binary-search-bounds": 3}], 12: [function (require, module, exports) {
        "use strict"

        function iota(n) {
            var result = new Array(n)
            for (var i = 0; i < n; ++i) {
                result[i] = i
            }
            return result
        }

        module.exports = iota
    }, {}], 13: [function (require, module, exports) {
        "use strict"

        var bipartiteIndependentSet = require("bipartite-independent-set")
        var createIntervalTree = require("interval-tree-1d")
        var dup = require("dup")

        module.exports = decomposeRegion
        global.decomposeRegion = decomposeRegion

        function Vertex(point, path, index, concave) {
            this.point = point
            this.path = path
            this.index = index
            this.concave = concave
            this.next = null
            this.prev = null
            this.visited = false
        }

        function Segment(start, end, direction) {
            var a = start.point[direction ^ 1]
            var b = end.point[direction ^ 1]
            if (a < b) {
                this[0] = a
                this[1] = b
            } else {
                this[0] = b
                this[1] = a
            }
            this.start = start
            this.end = end
            this.direction = direction
            this.number = -1
        }

        function testSegment(a, b, tree, direction) {
            var ax = a.point[direction ^ 1]
            var bx = b.point[direction ^ 1]
            return !!tree.queryPoint(a.point[direction], function (s) {
                var x = s.start.point[direction ^ 1]
                if (ax < x && x < bx) {
                    return true
                }
                return false
            })
        }

        function getDiagonals(vertices, paths, direction, tree) {
            var concave = []
            for (var i = 0; i < vertices.length; ++i) {
                if (vertices[i].concave) {
                    concave.push(vertices[i])
                }
            }
            concave.sort(function (a, b) {
                var d = a.point[direction] - b.point[direction]
                if (d) {
                    return d
                }
                return a.point[direction ^ 1] - b.point[direction ^ 1]
            })
            var diagonals = []
            for (var i = 1; i < concave.length; ++i) {
                var a = concave[i - 1]
                var b = concave[i]
                if (a.point[direction] === b.point[direction]) {
                    if (a.path === b.path) {
                        var n = paths[a.path].length
                        var d = (a.index - b.index + n) % n
                        if (d === 1 || d === n - 1) {
                            continue
                        }
                    }
                    if (!testSegment(a, b, tree, direction)) {
                        //Check orientation of diagonal
                        diagonals.push(new Segment(a, b, direction))
                    }
                }
            }
            return diagonals
        }

//Find all crossings between diagonals
        function findCrossings(hdiagonals, vdiagonals) {
            var htree = createIntervalTree(hdiagonals)
            var crossings = []
            for (var i = 0; i < vdiagonals.length; ++i) {
                var v = vdiagonals[i]
                var x = v.start.point[0]
                htree.queryPoint(v.start.point[1], function (h) {
                    var x = h.start.point[0]
                    if (v[0] <= x && x <= v[1]) {
                        crossings.push([h, v])
                    }
                })
            }
            return crossings
        }

        function findSplitters(hdiagonals, vdiagonals) {
            //First find crossings
            var crossings = findCrossings(hdiagonals, vdiagonals)

            //Then tag and convert edge format
            for (var i = 0; i < hdiagonals.length; ++i) {
                hdiagonals[i].number = i
            }
            for (var i = 0; i < vdiagonals.length; ++i) {
                vdiagonals[i].number = i
            }
            var edges = crossings.map(function (c) {
                return [c[0].number, c[1].number]
            })

            //Find independent set
            var selected = bipartiteIndependentSet(hdiagonals.length, vdiagonals.length, edges)

            //Convert into result format
            var result = new Array(selected[0].length + selected[1].length)
            var ptr = 0
            for (var i = 0; i < selected[0].length; ++i) {
                result[ptr++] = hdiagonals[selected[0][i]]
            }
            for (var i = 0; i < selected[1].length; ++i) {
                result[ptr++] = vdiagonals[selected[1][i]]
            }

            //Done
            return result
        }

        function splitSegment(segment) {
            //Store references
            var a = segment.start
            var b = segment.end
            var pa = a.prev
            var na = a.next
            var pb = b.prev
            var nb = b.next

            //Fix concavity
            a.concave = false
            b.concave = false

            //Compute orientation
            var ao = pa.point[segment.direction] === a.point[segment.direction]
            var bo = pb.point[segment.direction] === b.point[segment.direction]

            if (ao && bo) {
                //Case 1:
                //            ^
                //            |
                //  --->A+++++B<---
                //      |
                //      V
                a.prev = pb
                pb.next = a
                b.prev = pa
                pa.next = b
            } else if (ao && !bo) {
                //Case 2:
                //      ^     |
                //      |     V
                //  --->A+++++B--->
                //
                //
                a.prev = b
                b.next = a
                pa.next = nb
                nb.prev = pa
            } else if (!ao && bo) {
                //Case 3:
                //
                //
                //  <---A+++++B<---
                //      ^     |
                //      |     V
                a.next = b
                b.prev = a
                na.prev = pb
                pb.next = na

            } else if (!ao && !bo) {
                //Case 3:
                //            |
                //            V
                //  <---A+++++B--->
                //      ^
                //      |
                a.next = nb
                nb.prev = a
                b.next = na
                na.prev = b
            }
        }

        function findLoops(vertices) {
            //Initialize visit flag
            for (var i = 0; i < vertices.length; ++i) {
                vertices[i].visited = false
            }
            //Walk over vertex list
            var loops = []
            for (var i = 0; i < vertices.length; ++i) {
                var v = vertices[i]
                if (v.visited) {
                    continue
                }
                //Walk along loop
                var loop = []
                while (!v.visited) {
                    loop.push(v)
                    v.visited = true
                    v = v.next
                }
                loops.push(loop)
            }
            return loops
        }


        function splitConcave(vertices) {
            //First step: build segment tree from vertical segments
            var leftsegments = []
            var rightsegments = []
            for (var i = 0; i < vertices.length; ++i) {
                var v = vertices[i]
                if (v.next.point[1] === v.point[1]) {
                    if (v.next.point[0] < v.point[0]) {
                        leftsegments.push(new Segment(v, v.next, 1))
                    } else {
                        rightsegments.push(new Segment(v, v.next, 1))
                    }
                }
            }
            var lefttree = createIntervalTree(leftsegments)
            var righttree = createIntervalTree(rightsegments)
            for (var i = 0; i < vertices.length; ++i) {
                var v = vertices[i]
                if (!v.concave) {
                    continue
                }

                //Compute orientation
                var y = v.point[1]
                var direction
                if (v.prev.point[0] === v.point[0]) {
                    direction = v.prev.point[1] < y
                } else {
                    direction = v.next.point[1] < y
                }
                direction = direction ? 1 : -1

                //Scan a horizontal ray
                var closestSegment = null
                var closestDistance = Infinity * direction
                if (direction < 0) {
                    righttree.queryPoint(v.point[0], function (h) {
                        var x = h.start.point[1]
                        if (x < y && x > closestDistance) {
                            closestDistance = x
                            closestSegment = h
                        }
                    })
                } else {
                    lefttree.queryPoint(v.point[0], function (h) {
                        var x = h.start.point[1]
                        if (x > y && x < closestDistance) {
                            closestDistance = x
                            closestSegment = h
                        }
                    })
                }

                //Create two splitting vertices
                var splitA = new Vertex([v.point[0], closestDistance], 0, 0, false)
                var splitB = new Vertex([v.point[0], closestDistance], 0, 0, false)

                //Clear concavity flag
                v.concave = false

                if(closestSegment == null){
                    print(direction)
                    print(i)
                    print(v.point)
                    print("vertices")
                    for (var i = 0; i < vertices.length; ++i) {
                        var v = vertices[i]
                        print(v.point)
                    }
                }

                //Split vertices
                splitA.prev = closestSegment.start
                closestSegment.start.next = splitA
                splitB.next = closestSegment.end
                closestSegment.end.prev = splitB

                //Update segment tree
                var tree
                if (direction < 0) {
                    tree = righttree
                } else {
                    tree = lefttree
                }
                tree.remove(closestSegment)
                tree.insert(new Segment(closestSegment.start, splitA, 1))
                tree.insert(new Segment(splitB, closestSegment.end, 1))

                //Append vertices
                vertices.push(splitA, splitB)

                //Cut v, 2 different cases
                if (v.prev.point[0] === v.point[0]) {
                    // Case 1
                    //             ^
                    //             |
                    // --->*+++++++X
                    //     |       |
                    //     V       |
                    splitA.next = v
                    splitB.prev = v.prev
                } else {
                    // Case 2
                    //     |       ^
                    //     V       |
                    // <---*+++++++X
                    //             |
                    //             |
                    splitA.next = v.next
                    splitB.prev = v
                }

                //Fix up links
                splitA.next.prev = splitA
                splitB.prev.next = splitB
            }
        }

        function findRegions(vertices) {
            var n = vertices.length
            for (var i = 0; i < n; ++i) {
                vertices[i].visited = false
            }
            //Walk over vertex list
            var rectangles = []
            for (var i = 0; i < n; ++i) {
                var v = vertices[i]
                if (v.visited) {
                    continue
                }
                //Walk along loop
                var lo = [Infinity, Infinity]
                var hi = [-Infinity, -Infinity]
                while (!v.visited) {
                    for (var j = 0; j < 2; ++j) {
                        lo[j] = Math.min(v.point[j], lo[j])
                        hi[j] = Math.max(v.point[j], hi[j])
                    }
                    v.visited = true
                    v = v.next
                }
                rectangles.push([lo, hi])
            }
            return rectangles
        }


        function decomposeRegion(paths, clockwise) {
            var newPath = [];
            for (var k = 0; k < paths.length; ++k) {
                path = paths[k];
                var newLoop = [];
                for (var l = 0; l < path.length; ++l) {
                    newLoop[l] = [path[l][0], path[l][1]];
                }
                newPath[k] = newLoop;
            }
            paths = newPath;

            if (!Array.isArray(paths)) {
                throw new Error("rectangle-decomposition: Must specify list of loops")
            }

            //Coerce to boolean type
            clockwise = !!clockwise

            //First step: unpack all vertices into internal format
            var vertices = []
            var ptr = 0
            var npaths = new Array(paths.length)
            for (var i = 0; i < paths.length; ++i) {
                var path = paths[i]
                if (!Array.isArray(path)) {
                    throw new Error("rectangle-decomposition: Loop must be array type")
                }
                var n = path.length
                var prev = path[n - 3]
                var cur = path[n - 2]
                var next = path[n - 1]
                npaths[i] = []
                for (var j = 0; j < n; ++j) {
                    prev = cur
                    cur = next
                    next = path[j]
                    if (!Array.isArray(next) || next.length !== 2) {
                        throw new Error("rectangle-decomposition: Must specify list of loops")
                    }
                    var concave = false
                    if (prev[0] === cur[0]) {
                        if (next[0] === cur[0]) {
                            continue
                        }
                        var dir0 = prev[1] < cur[1]
                        var dir1 = cur[0] < next[0]
                        concave = dir0 === dir1
                    } else {
                        if (next[1] === cur[1]) {
                            continue
                        }
                        var dir0 = prev[0] < cur[0]
                        var dir1 = cur[1] < next[1]
                        concave = dir0 !== dir1
                    }
                    if (clockwise) {
                        concave = !concave
                    }
                    var vtx = new Vertex(
                        cur,
                        i,
                        (j + n - 1) % n,
                        concave)
                    npaths[i].push(vtx)
                    vertices.push(vtx)
                }
            }

            //Next build interval trees for segments, link vertices into a list
            var hsegments = []
            var vsegments = []
            for (var i = 0; i < npaths.length; ++i) {
                var p = npaths[i]
                for (var j = 0; j < p.length; ++j) {
                    var a = p[j]
                    var b = p[(j + 1) % p.length]
                    if (a.point[0] === b.point[0]) {
                        hsegments.push(new Segment(a, b, 0))
                    } else {
                        vsegments.push(new Segment(a, b, 1))
                    }
                    if (clockwise) {
                        a.prev = b
                        b.next = a
                    } else {
                        a.next = b
                        b.prev = a
                    }
                }
            }
            var htree = createIntervalTree(hsegments)
            var vtree = createIntervalTree(vsegments)

            //Find horizontal and vertical diagonals
            var hdiagonals = getDiagonals(vertices, npaths, 0, vtree)
            var vdiagonals = getDiagonals(vertices, npaths, 1, htree)

            //Find all splitting edges
            var splitters = findSplitters(hdiagonals, vdiagonals)

            //Cut all the splitting diagonals
            for (var i = 0; i < splitters.length; ++i) {
                splitSegment(splitters[i])
            }

            //Split all concave vertices
            splitConcave(vertices)

            //Return regions
            return findRegions(vertices)
        }
    }, {"bipartite-independent-set": 4, "dup": 9, "interval-tree-1d": 11}], 14: [function (require, module, exports) {
        (function (global, Buffer) {
            'use strict'

            var bits = require('bit-twiddle')
            var dup = require('dup')

//Legacy pool support
            if (!global.__TYPEDARRAY_POOL) {
                global.__TYPEDARRAY_POOL = {
                    UINT8: dup([32, 0])
                    , UINT16: dup([32, 0])
                    , UINT32: dup([32, 0])
                    , INT8: dup([32, 0])
                    , INT16: dup([32, 0])
                    , INT32: dup([32, 0])
                    , FLOAT: dup([32, 0])
                    , DOUBLE: dup([32, 0])
                    , DATA: dup([32, 0])
                    , UINT8C: dup([32, 0])
                    , BUFFER: dup([32, 0])
                }
            }

            var hasUint8C = (typeof Uint8ClampedArray) !== 'undefined'
            var POOL = global.__TYPEDARRAY_POOL

//Upgrade pool
            if (!POOL.UINT8C) {
                POOL.UINT8C = dup([32, 0])
            }
            if (!POOL.BUFFER) {
                POOL.BUFFER = dup([32, 0])
            }

//New technique: Only allocate from ArrayBufferView and Buffer
            var DATA = POOL.DATA
                , BUFFER = POOL.BUFFER

            exports.free = function free(array) {
                if (Buffer.isBuffer(array)) {
                    BUFFER[bits.log2(array.length)].push(array)
                } else {
                    if (Object.prototype.toString.call(array) !== '[object ArrayBuffer]') {
                        array = array.buffer
                    }
                    if (!array) {
                        return
                    }
                    var n = array.length || array.byteLength
                    var log_n = bits.log2(n) | 0
                    DATA[log_n].push(array)
                }
            }

            function freeArrayBuffer(buffer) {
                if (!buffer) {
                    return
                }
                var n = buffer.length || buffer.byteLength
                var log_n = bits.log2(n)
                DATA[log_n].push(buffer)
            }

            function freeTypedArray(array) {
                freeArrayBuffer(array.buffer)
            }

            exports.freeUint8 =
                exports.freeUint16 =
                    exports.freeUint32 =
                        exports.freeInt8 =
                            exports.freeInt16 =
                                exports.freeInt32 =
                                    exports.freeFloat32 =
                                        exports.freeFloat =
                                            exports.freeFloat64 =
                                                exports.freeDouble =
                                                    exports.freeUint8Clamped =
                                                        exports.freeDataView = freeTypedArray

            exports.freeArrayBuffer = freeArrayBuffer

            exports.freeBuffer = function freeBuffer(array) {
                BUFFER[bits.log2(array.length)].push(array)
            }

            exports.malloc = function malloc(n, dtype) {
                if (dtype === undefined || dtype === 'arraybuffer') {
                    return mallocArrayBuffer(n)
                } else {
                    switch (dtype) {
                        case 'uint8':
                            return mallocUint8(n)
                        case 'uint16':
                            return mallocUint16(n)
                        case 'uint32':
                            return mallocUint32(n)
                        case 'int8':
                            return mallocInt8(n)
                        case 'int16':
                            return mallocInt16(n)
                        case 'int32':
                            return mallocInt32(n)
                        case 'float':
                        case 'float32':
                            return mallocFloat(n)
                        case 'double':
                        case 'float64':
                            return mallocDouble(n)
                        case 'uint8_clamped':
                            return mallocUint8Clamped(n)
                        case 'buffer':
                            return mallocBuffer(n)
                        case 'data':
                        case 'dataview':
                            return mallocDataView(n)

                        default:
                            return null
                    }
                }
                return null
            }

            function mallocArrayBuffer(n) {
                var n = bits.nextPow2(n)
                var log_n = bits.log2(n)
                var d = DATA[log_n]
                if (d.length > 0) {
                    return d.pop()
                }
                return new ArrayBuffer(n)
            }

            exports.mallocArrayBuffer = mallocArrayBuffer

            function mallocUint8(n) {
                return new Uint8Array(mallocArrayBuffer(n), 0, n)
            }

            exports.mallocUint8 = mallocUint8

            function mallocUint16(n) {
                return new Uint16Array(mallocArrayBuffer(2 * n), 0, n)
            }

            exports.mallocUint16 = mallocUint16

            function mallocUint32(n) {
                return new Uint32Array(mallocArrayBuffer(4 * n), 0, n)
            }

            exports.mallocUint32 = mallocUint32

            function mallocInt8(n) {
                return new Int8Array(mallocArrayBuffer(n), 0, n)
            }

            exports.mallocInt8 = mallocInt8

            function mallocInt16(n) {
                return new Int16Array(mallocArrayBuffer(2 * n), 0, n)
            }

            exports.mallocInt16 = mallocInt16

            function mallocInt32(n) {
                return new Int32Array(mallocArrayBuffer(4 * n), 0, n)
            }

            exports.mallocInt32 = mallocInt32

            function mallocFloat(n) {
                return new Float32Array(mallocArrayBuffer(4 * n), 0, n)
            }

            exports.mallocFloat32 = exports.mallocFloat = mallocFloat

            function mallocDouble(n) {
                return new Float64Array(mallocArrayBuffer(8 * n), 0, n)
            }

            exports.mallocFloat64 = exports.mallocDouble = mallocDouble

            function mallocUint8Clamped(n) {
                if (hasUint8C) {
                    return new Uint8ClampedArray(mallocArrayBuffer(n), 0, n)
                } else {
                    return mallocUint8(n)
                }
            }

            exports.mallocUint8Clamped = mallocUint8Clamped

            function mallocDataView(n) {
                return new DataView(mallocArrayBuffer(n), 0, n)
            }

            exports.mallocDataView = mallocDataView

            function mallocBuffer(n) {
                n = bits.nextPow2(n)
                var log_n = bits.log2(n)
                var cache = BUFFER[log_n]
                if (cache.length > 0) {
                    return cache.pop()
                }
                return new Buffer(n)
            }

            exports.mallocBuffer = mallocBuffer

            exports.clearCache = function clearCache() {
                for (var i = 0; i < 32; ++i) {
                    POOL.UINT8[i].length = 0
                    POOL.UINT16[i].length = 0
                    POOL.UINT32[i].length = 0
                    POOL.INT8[i].length = 0
                    POOL.INT16[i].length = 0
                    POOL.INT32[i].length = 0
                    POOL.FLOAT[i].length = 0
                    POOL.DOUBLE[i].length = 0
                    POOL.UINT8C[i].length = 0
                    DATA[i].length = 0
                    BUFFER[i].length = 0
                }
            }
        }).call(this, typeof global !== "undefined" ? global : typeof self !== "undefined" ? self : typeof window !== "undefined" ? window : {}, require("buffer").Buffer)
    }, {"bit-twiddle": 7, "buffer": 8, "dup": 9}]
}, {}, [1]);


// var rectangle_decomposition = require("rectangle-decomposition")
