/**
 * 时间感知搜索工作流脚本
 * 
 * 此脚本定义了处理时间敏感查询的标准工作流程
 * 可在 Agent 中通过工具调用执行
 */

// 时间敏感关键词词典
const TIME_SENSITIVE_KEYWORDS = {
  // 相对时间词汇
  relative: ['最近', '最新', '当前', '现在', '目前', '现今', '近日', '近期'],
  // 时间段词汇
  period: ['近年来', '近几年', '近一段时间', 'lately', 'recently'],
  // 特定时间词汇
  specific: ['今年', '去年', '上月', '上周', '昨天', '今天', '明天'],
  // 时效性词汇
  timeliness: ['新闻', '热点', '趋势', '行情', '股价', '天气', '动态', '进展'],
  // 未来时间词汇
  future: ['即将', '将要', '未来', '预测', '展望', '计划']
};

// 时间词汇到时间范围的映射（单位：天）
const TIME_RANGE_MAP = {
  '现在': 0,
  '当前': 0,
  '目前': 0,
  '今天': 0,
  '昨天': 1,
  '最近': 7,
  '近日': 7,
  '近期': 7,
  '上周': 7,
  '上月': 30,
  '今年': 365,
  '去年': 365,
  '近年来': 1095,  // 3年
  '近几年': 1095
};

/**
 * 检测用户输入是否包含时间敏感词
 * @param {string} userInput - 用户输入
 * @returns {Object} - 检测结果
 */
function detectTimeSensitiveWords(userInput) {
  if (!userInput || typeof userInput !== 'string') {
    return { hasTimeSensitiveWords: false, keywords: [], level: 'low' };
  }
  
  const foundKeywords = [];
  let highestLevel = 'low';
  
  // 遍历所有关键词类别
  for (const [category, words] of Object.entries(TIME_SENSITIVE_KEYWORDS)) {
    for (const word of words) {
      if (userInput.includes(word)) {
        foundKeywords.push({ word, category });
        
        // 更新敏感度等级
        if (category === 'timeliness' || category === 'specific') {
          highestLevel = 'high';
        } else if (category === 'relative' && highestLevel !== 'high') {
          highestLevel = 'medium';
        }
      }
    }
  }
  
  return {
    hasTimeSensitiveWords: foundKeywords.length > 0,
    keywords: foundKeywords,
    level: highestLevel
  };
}

/**
 * 计算搜索时间范围
 * @param {string} keyword - 时间关键词
 * @param {Date} currentTime - 当前时间
 * @returns {Object} - 时间范围
 */
function calculateTimeRange(keyword, currentTime) {
  const days = TIME_RANGE_MAP[keyword] || 30; // 默认30天
  const endTime = new Date(currentTime);
  const startTime = new Date(currentTime);
  startTime.setDate(startTime.getDate() - days);
  
  return {
    startTime: startTime.toISOString().split('T')[0],
    endTime: endTime.toISOString().split('T')[0],
    days: days
  };
}

/**
 * 构建搜索查询
 * @param {string} userQuery - 用户原始查询
 * @param {string} timeKeyword - 时间关键词
 * @param {Date} currentTime - 当前时间
 * @returns {string} - 优化后的搜索查询
 */
function buildSearchQuery(userQuery, timeKeyword, currentTime) {
  const year = currentTime.getFullYear();
  const month = currentTime.getMonth() + 1;
  
  // 根据时间关键词构建查询
  let timeQualifier = '';
  
  if (['今年', '当前', '现在', '目前'].includes(timeKeyword)) {
    timeQualifier = `${year}年`;
  } else if (timeKeyword === '去年') {
    timeQualifier = `${year - 1}年`;
  } else if (timeKeyword === '上月') {
    const lastMonth = month === 1 ? 12 : month - 1;
    const lastMonthYear = month === 1 ? year - 1 : year;
    timeQualifier = `${lastMonthYear}年${lastMonth}月`;
  } else if (['最近', '最新', '近日', '近期'].includes(timeKeyword)) {
    timeQualifier = '最新';
  } else if (['近年来', '近几年'].includes(timeKeyword)) {
    timeQualifier = `${year - 3}年-${year}年`;
  }
  
  // 构建最终查询
  if (timeQualifier) {
    return `${userQuery} ${timeQualifier}`;
  }
  
  return userQuery;
}

/**
 * 执行完整的时间感知搜索工作流
 * @param {string} userInput - 用户输入
 * @returns {Object} - 工作流执行结果
 */
async function executeTimeAwareWorkflow(userInput) {
  // Step 1: 检测时间敏感词
  const detection = detectTimeSensitiveWords(userInput);
  
  if (!detection.hasTimeSensitiveWords) {
    return {
      needTimeAware: false,
      message: '未检测到时间敏感词，按常规流程处理'
    };
  }
  
  // Step 2: 获取当前时间（这里应该调用 DateTimeTool）
  const currentTime = new Date();
  
  // Step 3: 确定主要时间关键词
  const primaryKeyword = detection.keywords[0]?.word || '最近';
  
  // Step 4: 计算时间范围
  const timeRange = calculateTimeRange(primaryKeyword, currentTime);
  
  // Step 5: 构建搜索查询
  const searchQuery = buildSearchQuery(userInput, primaryKeyword, currentTime);
  
  return {
    needTimeAware: true,
    detection,
    currentTime,
    primaryKeyword,
    timeRange,
    searchQuery,
    nextSteps: [
      '调用 DateTimeTool 获取准确当前时间',
      `调用搜索工具，查询："${searchQuery}"`,
      '基于搜索结果生成时效性回答'
    ]
  };
}

// 导出函数供外部使用
module.exports = {
  detectTimeSensitiveWords,
  calculateTimeRange,
  buildSearchQuery,
  executeTimeAwareWorkflow,
  TIME_SENSITIVE_KEYWORDS,
  TIME_RANGE_MAP
};
