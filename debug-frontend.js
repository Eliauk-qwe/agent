// 前端调试脚本
const puppeteer = require('puppeteer');

(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  
  // 监听控制台消息
  page.on('console', msg => {
    console.log(`[${msg.type()}] ${msg.text()}`);
  });
  
  // 监听页面错误
  page.on('pageerror', error => {
    console.error('页面错误:', error.message);
  });
  
  // 监听请求失败
  page.on('requestfailed', request => {
    console.error('请求失败:', request.url(), request.failure().errorText);
  });
  
  try {
    await page.goto('http://localhost:5173');
    await page.waitForTimeout(3000);
    
    const title = await page.title();
    console.log('页面标题:', title);
    
    const content = await page.content();
    console.log('页面内容长度:', content.length);
    
  } catch (error) {
    console.error('访问页面失败:', error.message);
  }
  
  await browser.close();
})();