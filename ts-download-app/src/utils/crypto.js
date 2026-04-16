import JSEncrypt from 'jsencrypt'
import CryptoJS from 'crypto-js'

let publicKey = null

export async function fetchPublicKey(forceRefresh = false) {
  if (publicKey && !forceRefresh) return publicKey
  publicKey = null
  const response = await fetch('/api/crypto/publicKey')
  const data = await response.json()
  if (data.code === 200) {
    publicKey = data.data
  }
  return publicKey
}

export function clearPublicKey() {
  publicKey = null
}

function generateAesKey() {
  return CryptoJS.lib.WordArray.random(16)
}

function generateIv() {
  return CryptoJS.lib.WordArray.random(16)
}

export function encryptRequest(data) {
  if (!publicKey) return null

  const aesKey = generateAesKey()
  const iv = generateIv()

  const encrypted = CryptoJS.AES.encrypt(
    typeof data === 'string' ? data : JSON.stringify(data),
    aesKey,
    { iv, mode: CryptoJS.mode.CBC, padding: CryptoJS.pad.Pkcs7 }
  )

  const aesKeyBase64 = CryptoJS.enc.Base64.stringify(aesKey)
  const ivBase64 = CryptoJS.enc.Base64.stringify(iv)

  const jsEncrypt = new JSEncrypt()
  jsEncrypt.setPublicKey(publicKey)
  const encryptedKey = jsEncrypt.encrypt(aesKeyBase64)

  return {
    encryptedData: encrypted.toString(),
    encryptedKey,
    iv: ivBase64,
    aesKeyBase64,
    aesKey,
    ivWordArray: iv
  }
}

export function decryptResponse(encryptedData, aesKeyBase64, ivBase64) {
  const key = CryptoJS.enc.Base64.parse(aesKeyBase64)
  const iv = CryptoJS.enc.Base64.parse(ivBase64)

  const decrypted = CryptoJS.AES.decrypt(encryptedData, key, {
    iv,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  })

  return decrypted.toString(CryptoJS.enc.Utf8)
}
