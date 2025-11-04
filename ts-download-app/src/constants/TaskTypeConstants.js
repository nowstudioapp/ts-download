/**
 * 任务类型常量
 * 
 * @author TS Team
 */

export const TaskTypeConstants = {
  // 性别年龄筛选
  GENDER: 'gender',
  
  // WhatsApp存在性检测(实时)
  DSP_WHATSAPP_EXIST: 'whatsappExist',
  
  // WhatsApp存在性检测(撞库)
  WS_EXIST: 'wsExist',

  // WhatsApp有效性检测
  WS_VALID: 'wsValid',

  // iOS消息
  IOS_MESSAGE: 'iMessage',

  // Telegram有效性检测
  TG_EFFECTIVE: 'tgEffective',

  // Telegram筛选活跃
  TNT_SIEVE_LIVE: 'sieveLive',

  // Telegram筛选头像
  TNT_SIEVE_AVATAR: 'sieveAvatar',

  // Facebook邮箱
  FB_EMAIL: 'fbEmail',

  // Facebook手机号
  FB_PHONE: 'fbPhone',

  // Binance手机号
  BINANCE_PHONE: 'binancePhone',

  // iOS有效性
  IOS_VALID: 'iosValid',

  // RCS有效性
  RCS_VALID: 'rcsValid',

  // 巴西手机号
  BR_PHONE: 'brPhone',

  // 全球运营商
  GLOBAL_OPERATORS_PHONE: 'globalOperatorsPhone',

  // Line
  WASFISH_LINE: 'line',

  // Line性别
  WASFISH_LINE_GENDER: 'line_gender',

  // Amazon
  WASFISH_AMAZON: 'amazon',

  // Zalo
  WASFISH_ZALO: 'zalo',

  // Viber
  WASFISH_VIBER: 'viber',

  // Viber活跃
  WASFISH_VIBER_ACTIVE: 'viber_active',

  // Instagram
  INS: 'ins',

  // 美国运营商
  USA_CARRIER: 'usaCarrier',

  // 手机号检测
  PHONE_CHECK: 'phoneCheck',

  // Microsoft
  MICROSOFT: 'microsoft',

  // India Times
  INDIATIMES: 'indiatimes',

  // Paytm
  PAYTM: 'paytm',

  // EaseMoin
  EASEMOIN: 'easeMoni',

  // DHL
  DHL: 'dhl',

  // MoniePoint
  MONIEPOINT: 'moniepoint',

  // OPay
  OPAY: 'opay',

  // Economic Times
  ECONOMICTIMES: 'economictimes',

  // Band
  BAND: 'band',

  // Rummy Circle
  RUMMYCIRCLE: 'rummyCircle',

  // KuCoin
  KUCOIN: 'kucoin',

  // Momo
  MOMO: 'momo',

  // Shine
  SHINE: 'shine',

  // Swiggy
  SWIGGY: 'swiggy',

  // MakeMyTrip
  MAKEMYTRIP: 'makemytrip',

  // Vantage
  VANTAGE: 'vantage',

  // Cian
  CIAN: 'cian',

  // Noon
  NOON: 'noon',

  // Grab
  GRAB: 'grab',

  // Check24
  CHECK24: 'check24',

  // 5Paisa
  PAISA_5: '5paisa',

  // CaratLane
  CARATLANE: 'caratlane',

  // HTX
  HTX: 'htx',

  // Twitter
  TW: 'tw',

  // 越南运营商
  VNCARRIER: 'vnCarrier',

  // 德国运营商
  DECARRIER: 'deCarrier',

  // 法国运营商
  FRCARRIER: 'frCarrier',

  // 俄罗斯运营商
  RUCARRIER: 'ruCarrier',

  // 巴基斯坦运营商
  PKCARRIER: 'pkCarrier',

  // 巴西运营商
  BRCARRIER: 'brCarrier',

  // 日本运营商
  JPCARRIER: 'jpCarrier',

  // 英国运营商
  GBCARRIER: 'gbCarrier',

  // 孟加拉国运营商
  BDCARRIER: 'bdCarrier',

  // 土耳其运营商
  TRCARRIER: 'trCarrier',

  // 印度尼西亚运营商
  IDCARRIER: 'idCarrier',

  // 摩洛哥运营商
  MACARRIER: 'maCarrier',

  // 乌克兰运营商
  UACARRIER: 'uaCarrier',

  // 乌兹别克斯坦运营商
  UZCARRIER: 'uzCarrier',

  // 哈萨克斯坦运营商
  KZCARRIER: 'kzCarrier',

  // 西班牙运营商
  ESCARRIER: 'esCarrier',

  // 荷兰运营商
  NLCARRIER: 'nlCarrier',

  // 墨西哥运营商
  MXCARRIER: 'mxCarrier',

  // 葡萄牙运营商
  PTCARRIER: 'ptCarrier',

  // Facebook Messenger
  FBMESSAGER: 'fbMessager',

  // Binance邮箱
  BINANCE_EMAIL: 'binanceEmail',

  // Robinhood邮箱
  ROBINHOOD_EMAIL: 'robinhoodEmail',

  // KuCoin邮箱
  KUCOIN_EMAIL: 'kucoinEmail',

  // XT手机号
  XT_PHONE: 'xtPhone',

  // XT邮箱
  XT_EMAIL: 'xtEmail',

  // HTX邮箱
  HTX_EMAIL: 'htxEmail',

  // CoinW手机号
  COINW_PHONE: 'coinwPhone',

  // CoinW邮箱
  COINW_EMAIL: 'coinwEmail',

  // 全球邮箱实时检测
  EMAIL: 'email'
};

// 任务类型选项列表（用于前端显示）
export const TaskTypeOptions = [
  // 常用任务类型
  { label: '性别识别', value: 'gender', category: 'common' },
  { label: 'WS有效验证', value: 'wsValid', category: 'common' },
  { label: 'iOS有效验证', value: 'iosValid', category: 'common' },
  { label: 'RCS有效验证', value: 'rcsValid', category: 'common' },
  
  // WhatsApp相关
  { label: 'WS存在检测(实时)', value: 'whatsappExist', category: 'whatsapp' },
  { label: 'WS存在检测(撞库)', value: 'wsExist', category: 'whatsapp' },
  
  // iOS相关
  { label: 'iOS消息', value: 'iMessage', category: 'ios' },
  
  // Telegram相关
  { label: 'TG有效筛选', value: 'tgEffective', category: 'telegram' },
  { label: 'TG活跃筛选', value: 'sieveLive', category: 'telegram' },
  { label: 'TG头像筛选', value: 'sieveAvatar', category: 'telegram' },
  
  // Facebook相关
  { label: 'FB邮箱', value: 'fbEmail', category: 'facebook' },
  { label: 'FB手机号', value: 'fbPhone', category: 'facebook' },
  { label: 'FB Messenger', value: 'fbMessager', category: 'facebook' },
  
  // 社交媒体
  { label: 'Instagram', value: 'ins', category: 'social' },
  { label: 'Twitter', value: 'tw', category: 'social' },
  { label: 'Line开通', value: 'line', category: 'social' },
  { label: 'Line性别', value: 'line_gender', category: 'social' },
  { label: 'Viber开通', value: 'viber', category: 'social' },
  { label: 'Viber活跃', value: 'viber_active', category: 'social' },
  { label: 'Zalo开通', value: 'zalo', category: 'social' },
  
  // 电商平台
  { label: 'Amazon开通', value: 'amazon', category: 'ecommerce' },
  { label: 'Grab', value: 'grab', category: 'ecommerce' },
  { label: 'Noon', value: 'noon', category: 'ecommerce' },
  { label: 'Swiggy', value: 'swiggy', category: 'ecommerce' },
  
  // 金融交易
  { label: 'Binance手机号', value: 'binancePhone', category: 'finance' },
  { label: 'Binance邮箱', value: 'binanceEmail', category: 'finance' },
  { label: 'KuCoin', value: 'kucoin', category: 'finance' },
  { label: 'KuCoin邮箱', value: 'kucoinEmail', category: 'finance' },
  { label: 'HTX', value: 'htx', category: 'finance' },
  { label: 'HTX邮箱', value: 'htxEmail', category: 'finance' },
  { label: 'XT手机号', value: 'xtPhone', category: 'finance' },
  { label: 'XT邮箱', value: 'xtEmail', category: 'finance' },
  { label: 'CoinW手机号', value: 'coinwPhone', category: 'finance' },
  { label: 'CoinW邮箱', value: 'coinwEmail', category: 'finance' },
  { label: 'Robinhood邮箱', value: 'robinhoodEmail', category: 'finance' },
  { label: 'Vantage', value: 'vantage', category: 'finance' },
  { label: '5Paisa', value: '5paisa', category: 'finance' },
  
  // 运营商检测
  { label: '美国运营商', value: 'usaCarrier', category: 'carrier' },
  { label: '越南运营商', value: 'vnCarrier', category: 'carrier' },
  { label: '德国运营商', value: 'deCarrier', category: 'carrier' },
  { label: '法国运营商', value: 'frCarrier', category: 'carrier' },
  { label: '俄罗斯运营商', value: 'ruCarrier', category: 'carrier' },
  { label: '巴基斯坦运营商', value: 'pkCarrier', category: 'carrier' },
  { label: '巴西运营商', value: 'brCarrier', category: 'carrier' },
  { label: '日本运营商', value: 'jpCarrier', category: 'carrier' },
  { label: '英国运营商', value: 'gbCarrier', category: 'carrier' },
  { label: '孟加拉运营商', value: 'bdCarrier', category: 'carrier' },
  { label: '土耳其运营商', value: 'trCarrier', category: 'carrier' },
  { label: '印尼运营商', value: 'idCarrier', category: 'carrier' },
  { label: '摩洛哥运营商', value: 'maCarrier', category: 'carrier' },
  { label: '乌克兰运营商', value: 'uaCarrier', category: 'carrier' },
  { label: '乌兹别克斯坦运营商', value: 'uzCarrier', category: 'carrier' },
  { label: '哈萨克斯坦运营商', value: 'kzCarrier', category: 'carrier' },
  { label: '西班牙运营商', value: 'esCarrier', category: 'carrier' },
  { label: '荷兰运营商', value: 'nlCarrier', category: 'carrier' },
  { label: '墨西哥运营商', value: 'mxCarrier', category: 'carrier' },
  { label: '葡萄牙运营商', value: 'ptCarrier', category: 'carrier' },
  
  // 其他服务
  { label: '全球运营商', value: 'globalOperatorsPhone', category: 'other' },
  { label: '巴西手机号', value: 'brPhone', category: 'other' },
  { label: '手机号检测', value: 'phoneCheck', category: 'other' },
  { label: '全球邮箱检测', value: 'email', category: 'other' },
  { label: 'Microsoft', value: 'microsoft', category: 'other' },
  { label: 'India Times', value: 'indiatimes', category: 'other' },
  { label: 'Paytm', value: 'paytm', category: 'other' },
  { label: 'EaseMoni', value: 'easeMoni', category: 'other' },
  { label: 'DHL', value: 'dhl', category: 'other' },
  { label: 'MoniePoint', value: 'moniepoint', category: 'other' },
  { label: 'OPay', value: 'opay', category: 'other' },
  { label: 'Economic Times', value: 'economictimes', category: 'other' },
  { label: 'Band', value: 'band', category: 'other' },
  { label: 'Rummy Circle', value: 'rummyCircle', category: 'other' },
  { label: 'Momo', value: 'momo', category: 'other' },
  { label: 'Shine', value: 'shine', category: 'other' },
  { label: 'MakeMyTrip', value: 'makemytrip', category: 'other' },
  { label: 'Cian', value: 'cian', category: 'other' },
  { label: 'Check24', value: 'check24', category: 'other' },
  { label: 'CaratLane', value: 'caratlane', category: 'other' }
];

// 常用任务类型
export const PopularTaskTypes = [
  { label: '性别识别', value: 'gender' },
  { label: 'WS有效验证', value: 'wsValid' },
  { label: 'iOS有效验证', value: 'iosValid' },
  { label: 'RCS有效验证', value: 'rcsValid' },
  { label: 'TG有效筛选', value: 'tgEffective' },
  { label: 'Instagram', value: 'ins' },
  { label: 'FB手机号', value: 'fbPhone' },
  { label: 'Binance手机号', value: 'binancePhone' }
];

export default TaskTypeConstants;
