const { createProxyMiddleware } = require('http-proxy-middleware');

module.exports = function(app) {
  app.use(
    '/api',
    createProxyMiddleware({
      target: 'http://200.16.7.181',
      changeOrigin: true,
      secure: false,
      onProxyReq: (proxyReq, req, res) => {
        console.log('Proxy request:', req.method, req.path);
      },
      onError: (err, req, res) => {
        console.error('Proxy error:', err);
      }
    })
  );
};
