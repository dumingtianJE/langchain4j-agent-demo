package com.yourcompany.langchain4j.knowledge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 前端全栈架构模范代码
 * 补充 Vue/React 前端工程化、微前端、SSR 等架构示例
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FrontendArchitectureKnowledge implements CommandLineRunner {
    
    private final KnowledgeBaseManager knowledgeBaseManager;
    
    @Override
    public void run(String... args) {
        loadFrontendArchitecturePatterns();
    }
    
    private void loadFrontendArchitecturePatterns() {
        // 1. Vue 3 企业级项目架构
        knowledgeBaseManager.addDocument(new KnowledgeDocument(
            "vue3-enterprise-architecture",
            "Vue 3：企业级前端项目架构",
            """
            【Vue 3 企业级项目架构】
            
            ## 1. 项目目录结构
            
            \`\`\`
            src/
            ├── api/              # API 接口层
            │   ├── modules/      # 按模块划分
            │   │   ├── user.js
            │   │   ├── order.js
            │   │   └── product.js
            │   └── index.js      # API 统一导出
            ├── assets/           # 静态资源
            │   ├── images/
            │   ├── styles/
            │   │   ├── variables.scss
            │   │   ├── mixins.scss
            │   │   └── global.scss
            │   └── icons/
            ├── components/       # 公共组件
            │   ├── common/       # 基础组件
            │   │   ├── BaseButton.vue
            │   │   ├── BaseInput.vue
            │   │   └── BaseTable.vue
            │   ├── business/     # 业务组件
            │   │   ├── OrderCard.vue
            │   │   └── ProductList.vue
            │   └── layout/       # 布局组件
            │       ├── Header.vue
            │       ├── Sidebar.vue
            │       └── Footer.vue
            ├── composables/      # 组合式函数
            │   ├── useAuth.js
            │   ├── usePagination.js
            │   └── useWebSocket.js
            ├── router/           # 路由配置
            │   ├── index.js
            │   ├── routes.js
            │   └── guards.js     # 路由守卫
            ├── store/            # Pinia 状态管理
            │   ├── modules/
            │   │   ├── user.js
            │   │   ├── order.js
            │   │   └── app.js
            │   └── index.js
            ├── utils/            # 工具函数
            │   ├── request.js    # Axios 封装
            │   ├── auth.js
            │   ├── storage.js
            │   └── validators.js
            ├── views/            # 页面组件
            │   ├── Home/
            │   ├── Order/
            │   └── User/
            ├── App.vue
            └── main.js
            \`\`\`
            
            ## 2. Axios 请求封装
            
            \`\`\`javascript
            // utils/request.js
            import axios from 'axios';
            import { ElMessage } from 'element-plus';
            import router from '@/router';
            import { getToken, removeToken } from '@/utils/auth';
            
            const service = axios.create({
              baseURL: import.meta.env.VITE_API_BASE_URL,
              timeout: 15000
            });
            
            // 请求拦截器
            service.interceptors.request.use(
              config => {
                const token = getToken();
                if (token) {
                  config.headers.Authorization = `Bearer ${token}`;
                }
                return config;
              },
              error => {
                console.error('Request error:', error);
                return Promise.reject(error);
              }
            );
            
            // 响应拦截器
            service.interceptors.response.use(
              response => {
                const res = response.data;
                
                if (res.code !== 200) {
                  ElMessage.error(res.message || '请求失败');
                  
                  // 401: 未授权
                  if (res.code === 401) {
                    removeToken();
                    router.push('/login');
                  }
                  
                  return Promise.reject(new Error(res.message));
                }
                
                return res;
              },
              error => {
                console.error('Response error:', error);
                
                let message = '网络异常';
                if (error.response) {
                  switch (error.response.status) {
                    case 400: message = '请求参数错误'; break;
                    case 401: 
                      removeToken();
                      router.push('/login');
                      message = '未授权，请重新登录';
                      break;
                    case 403: message = '拒绝访问'; break;
                    case 404: message = '请求资源不存在'; break;
                    case 500: message = '服务器错误'; break;
                    default: message = `请求失败: ${error.response.status}`;
                  }
                }
                
                ElMessage.error(message);
                return Promise.reject(error);
              }
            );
            
            export default service;
            \`\`\`
            
            ## 3. API 模块化
            
            \`\`\`javascript
            // api/modules/order.js
            import request from '@/utils/request';
            
            export function getOrders(params) {
              return request({
                url: '/api/orders',
                method: 'get',
                params
              });
            }
            
            export function createOrder(data) {
              return request({
                url: '/api/orders',
                method: 'post',
                data
              });
            }
            
            export function getOrderDetail(id) {
              return request({
                url: `/api/orders/${id}`,
                method: 'get'
              });
            }
            
            export function updateOrderStatus(id, status) {
              return request({
                url: `/api/orders/${id}/status`,
                method: 'put',
                data: { status }
              });
            }
            \`\`\`
            
            ## 4. Pinia 状态管理
            
            \`\`\`javascript
            // store/modules/user.js
            import { defineStore } from 'pinia';
            import { login, getUserInfo } from '@/api/modules/user';
            import { setToken, getToken, removeToken } from '@/utils/auth';
            
            export const useUserStore = defineStore('user', {
              state: () => ({
                token: getToken(),
                userInfo: null,
                roles: []
              }),
              
              getters: {
                isLoggedIn: state => !!state.token,
                username: state => state.userInfo?.username || ''
              },
              
              actions: {
                async login(loginForm) {
                  const res = await login(loginForm);
                  this.token = res.data.token;
                  setToken(res.data.token);
                },
                
                async getUserInfo() {
                  const res = await getUserInfo();
                  this.userInfo = res.data.userInfo;
                  this.roles = res.data.roles;
                  return res.data;
                },
                
                async logout() {
                  this.token = null;
                  this.userInfo = null;
                  this.roles = [];
                  removeToken();
                }
              }
            });
            \`\`\`
            
            ## 5. 组合式函数封装
            
            \`\`\`javascript
            // composables/usePagination.js
            import { ref, computed } from 'vue';
            
            export function usePagination(fetchFn, defaultParams = {}) {
              const loading = ref(false);
              const data = ref([]);
              const total = ref(0);
              const currentPage = ref(1);
              const pageSize = ref(20);
              
              const totalPages = computed(() => Math.ceil(total.value / pageSize.value));
              
              async function fetchData(params = {}) {
                loading.value = true;
                try {
                  const res = await fetchFn({
                    ...defaultParams,
                    ...params,
                    page: currentPage.value,
                    size: pageSize.value
                  });
                  data.value = res.data.list;
                  total.value = res.data.total;
                } finally {
                  loading.value = false;
                }
              }
              
              function handlePageChange(page) {
                currentPage.value = page;
                fetchData();
              }
              
              function handleSizeChange(size) {
                pageSize.value = size;
                currentPage.value = 1;
                fetchData();
              }
              
              return {
                loading,
                data,
                total,
                currentPage,
                pageSize,
                totalPages,
                fetchData,
                handlePageChange,
                handleSizeChange
              };
            }
            \`\`\`
            
            ## 6. 路由配置和守卫
            
            \`\`\`javascript
            // router/guards.js
            import { useUserStore } from '@/store/modules/user';
            import { getToken } from '@/utils/auth';
            
            export function setupRouterGuard(router) {
              router.beforeEach(async (to, from, next) => {
                const userStore = useUserStore();
                const hasToken = getToken();
                
                if (hasToken) {
                  if (to.path === '/login') {
                    next({ path: '/' });
                  } else if (!userStore.userInfo) {
                    try {
                      await userStore.getUserInfo();
                      next({ ...to, replace: true });
                    } catch (error) {
                      await userStore.logout();
                      next(`/login?redirect=${to.path}`);
                    }
                  } else {
                    next();
                  }
                } else {
                  if (to.meta.requiresAuth) {
                    next(`/login?redirect=${to.path}`);
                  } else {
                    next();
                  }
                }
              });
            }
            \`\`\`
            
            ## 关键要点
            
            1. **模块化组织**：按功能模块划分代码
            2. **请求封装**：统一拦截器处理认证和错误
            3. **状态管理**：Pinia 替代 Vuex
            4. **组合式 API**：逻辑复用和封装
            5. **路由守卫**：权限控制核心
            """,
            "代码示例",
            new String[]{"vue3", "frontend", "architecture", "pinia", "composition-api"},
            "system",
            null,
            java.time.LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        // 2. React 企业级项目架构
        knowledgeBaseManager.addDocument(new KnowledgeDocument(
            "react-enterprise-architecture",
            "React：企业级前端项目架构",
            """
            【React 企业级项目架构】
            
            ## 1. 项目目录结构
            
            \`\`\`
            src/
            ├── api/              # API 层
            │   ├── http.ts       # Axios 封装
            │   └── services/     # 业务 API
            ├── components/       # 组件
            │   ├── common/       # 通用组件
            │   ├── business/     # 业务组件
            │   └── layout/       # 布局组件
            ├── hooks/            # 自定义 Hooks
            ├── pages/            # 页面组件
            ├── store/            # Redux/Recoil 状态管理
            │   ├── slices/
            │   └── index.ts
            ├── routes/           # 路由配置
            ├── utils/            # 工具函数
            ├── styles/           # 样式文件
            ├── types/            # TypeScript 类型定义
            ├── App.tsx
            └── main.tsx
            \`\`\`
            
            ## 2. TypeScript API 封装
            
            \`\`\`typescript
            // api/http.ts
            import axios, { AxiosRequestConfig, AxiosResponse } from 'axios';
            import { message } from 'antd';
            
            interface ApiResponse<T = any> {
              code: number;
              data: T;
              message: string;
            }
            
            const http = axios.create({
              baseURL: import.meta.env.VITE_API_BASE_URL,
              timeout: 15000
            });
            
            // 请求拦截器
            http.interceptors.request.use(
              (config) => {
                const token = localStorage.getItem('token');
                if (token) {
                  config.headers.Authorization = `Bearer ${token}`;
                }
                return config;
              },
              (error) => Promise.reject(error)
            );
            
            // 响应拦截器
            http.interceptors.response.use(
              (response: AxiosResponse<ApiResponse>) => {
                const res = response.data;
                if (res.code !== 200) {
                  message.error(res.message);
                  return Promise.reject(new Error(res.message));
                }
                return res;
              },
              (error) => {
                message.error(error.message || '网络异常');
                return Promise.reject(error);
              }
            );
            
            export default http;
            \`\`\`
            
            ## 3. RTK Query 数据获取
            
            \`\`\`typescript
            // store/services/orderApi.ts
            import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
            import { Order, OrderListParams } from '@/types';
            
            export const orderApi = createApi({
              reducerPath: 'orderApi',
              baseQuery: fetchBaseQuery({ baseUrl: '/api' }),
              tagTypes: ['Order'],
              endpoints: (builder) => ({
                getOrders: builder.query<Order[], OrderListParams>({
                  query: (params) => ({
                    url: '/orders',
                    params
                  }),
                  providesTags: (result) => 
                    result
                      ? [...result.map(({ id }) => ({ type: 'Order' as const, id })), { type: 'Order', id: 'LIST' }]
                      : [{ type: 'Order', id: 'LIST' }]
                }),
                
                createOrder: builder.mutation<Order, Partial<Order>>({
                  query: (body) => ({
                    url: '/orders',
                    method: 'POST',
                    body
                  }),
                  invalidatesTags: [{ type: 'Order', id: 'LIST' }]
                }),
                
                updateOrder: builder.mutation<Order, { id: number; body: Partial<Order> }>({
                  query: ({ id, body }) => ({
                    url: `/orders/${id}`,
                    method: 'PUT',
                    body
                  }),
                  invalidatesTags: (result, error, { id }) => [{ type: 'Order', id }]
                })
              })
            });
            
            export const {
              useGetOrdersQuery,
              useCreateOrderMutation,
              useUpdateOrderMutation
            } = orderApi;
            \`\`\`
            
            ## 4. 自定义 Hooks
            
            \`\`\`typescript
            // hooks/useAuth.ts
            import { useState, useEffect } from 'react';
            import { useNavigate } from 'react-router-dom';
            import { useAppDispatch, useAppSelector } from '@/store';
            import { login, logout, getUserInfo } from '@/store/slices/authSlice';
            
            export function useAuth() {
              const dispatch = useAppDispatch();
              const navigate = useNavigate();
              const { user, token, isLoading } = useAppSelector(state => state.auth);
              
              const [isAuthenticated, setIsAuthenticated] = useState(!!token);
              
              useEffect(() => {
                setIsAuthenticated(!!token);
              }, [token]);
              
              const handleLogin = async (credentials: LoginCredentials) => {
                await dispatch(login(credentials)).unwrap();
                await dispatch(getUserInfo()).unwrap();
                navigate('/');
              };
              
              const handleLogout = async () => {
                await dispatch(logout()).unwrap();
                navigate('/login');
              };
              
              return {
                user,
                token,
                isLoading,
                isAuthenticated,
                login: handleLogin,
                logout: handleLogout
              };
            }
            \`\`\`
            
            ## 5. 路由配置
            
            \`\`\`typescript
            // routes/index.tsx
            import { createBrowserRouter, Navigate } from 'react-router-dom';
            import AppLayout from '@/components/layout/AppLayout';
            import LoginPage from '@/pages/Login';
            import OrderList from '@/pages/Order/List';
            import OrderDetail from '@/pages/Order/Detail';
            import ProtectedRoute from './ProtectedRoute';
            
            const router = createBrowserRouter([
              {
                path: '/login',
                element: <LoginPage />
              },
              {
                path: '/',
                element: (
                  <ProtectedRoute>
                    <AppLayout />
                  </ProtectedRoute>
                ),
                children: [
                  {
                    index: true,
                    element: <Navigate to="/orders" replace />
                  },
                  {
                    path: 'orders',
                    children: [
                      {
                        index: true,
                        element: <OrderList />
                      },
                      {
                        path: ':id',
                        element: <OrderDetail />
                      }
                    ]
                  }
                ]
              }
            ]);
            
            export default router;
            \`\`\`
            
            ## 关键要点
            
            1. **TypeScript**：类型安全
            2. **RTK Query**：数据获取和缓存
            3. **Hooks**：逻辑复用
            4. **路由保护**：认证守卫
            5. **组件拆分**：原子化设计
            """,
            "代码示例",
            new String[]{"react", "typescript", "rtk-query", "hooks", "frontend"},
            "system",
            null,
            java.time.LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        // 3. 微前端架构
        knowledgeBaseManager.addDocument(new KnowledgeDocument(
            "micro-frontend-architecture",
            "微前端：qiankun 框架实战",
            """
            【微前端架构 - qiankun】
            
            ## 1. 主应用配置
            
            \`\`\`javascript
            // main/src/main.js
            import { registerMicroApps, start } from 'qiankun';
            
            registerMicroApps([
              {
                name: 'order-app',
                entry: '//localhost:8081',
                container: '#subapp-container',
                activeRule: '/orders',
                props: {
                  token: '主应用传递的 token',
                  onGlobalStateChange: (state) => console.log(state)
                }
              },
              {
                name: 'user-app',
                entry: '//localhost:8082',
                container: '#subapp-container',
                activeRule: '/users'
              },
              {
                name: 'product-app',
                entry: '//localhost:8083',
                container: '#subapp-container',
                activeRule: '/products'
              }
            ], {
              beforeLoad: app => {
                console.log('before load', app.name);
                return Promise.resolve();
              },
              beforeMount: app => {
                console.log('before mount', app.name);
                return Promise.resolve();
              },
              afterMount: app => {
                console.log('after mount', app.name);
                return Promise.resolve();
              }
            });
            
            start({
              prefetch: 'all',
              sandbox: {
                strictStyleIsolation: true,
                experimentalStyleIsolation: true
              }
            });
            \`\`\`
            
            ## 2. 子应用配置
            
            \`\`\`javascript
            // sub-app/src/public-path.js
            if (window.__POWERED_BY_QIANKUN__) {
              __webpack_public_path__ = window.__INJECTED_PUBLIC_PATH_BY_QIANKUN__;
            }
            
            // sub-app/src/main.js
            import './public-path';
            import { createApp } from 'vue';
            import App from './App.vue';
            import router from './router';
            
            let instance = null;
            
            function render(props = {}) {
              const { container } = props;
              instance = createApp(App);
              instance.use(router);
              instance.mount(container ? container.querySelector('#app') : '#app');
            }
            
            // 独立运行时
            if (!window.__POWERED_BY_QIANKUN__) {
              render();
            }
            
            export async function bootstrap() {
              console.log('vue app bootstraped');
            }
            
            export async function mount(props) {
              render(props);
            }
            
            export async function unmount() {
              instance.unmount();
              instance.$destroy();
              instance = null;
            }
            \`\`\`
            
            ## 3. 全局状态管理
            
            \`\`\`javascript
            // main/src/store.js
            import { initGlobalState } from 'qiankun';
            
            const initialState = {
              user: null,
              token: '',
              permissions: []
            };
            
            const actions = initGlobalState(initialState);
            
            actions.onGlobalStateChange((state, prev) => {
              console.log('全局状态变化', state, prev);
            });
            
            export function setGlobalState(state) {
              actions.setGlobalState(state);
            }
            
            export function getGlobalState() {
              return actions.getGlobalState();
            }
            \`\`\`
            
            ## 4. 样式隔离方案
            
            \`\`\`css
            /* 子应用使用 CSS Modules */
            .button {
              /* 自动添加哈希前缀 */
            }
            
            /* 或使用 Shadow DOM */
            {
              sandbox: {
                strictStyleIsolation: true
              }
            }
            \`\`\`
            
            ## 关键要点
            
            1. **应用隔离**：JS 沙箱 + 样式隔离
            2. **路由同步**：主应用控制子应用路由
            3. **状态共享**：全局状态管理
            4. **独立部署**：子应用可独立开发部署
            5. **通信机制**：Props + 全局状态
            """,
            "代码示例",
            new String[]{"micro-frontend", "qiankun", "architecture", "vue", "react"},
            "system",
            null,
            java.time.LocalDateTime.now(),
            null,
            0,
            null
        ));
        
        log.info("前端全栈架构模范代码知识库加载完成");
    }
}
