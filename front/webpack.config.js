// Common import
const fs = require('fs');


// DotsGame import
const webpack = require('webpack');
const VueLoader = require('vue-loader')
const vueFormsModuleTransform = require('./src/common/Forms/vue-forms-module');
const vueVarModuleTransform = require('./src/common/vue-var-module');
const WebpackPwaManifest = require('webpack-pwa-manifest');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const babelConfigJs = require('./babel.config')

const TITLE = 'DotsGame';
const VERSION = ((def) => {
    let file;
    try {
        file = fs.readFileSync(__dirname + '/../version.txt')
    } catch (e) {
        console.warn('Read version error: ', e);
        return def
    }
    return file.toString().trim()
})('0.0');

const devServerPublic = '127.0.0.1:8083';
const devBackend = 'http://127.0.0.1:8082';
const staticRootPath = __dirname + '/root/';

function DotsGameConfig(env, _) {
    const mode = (_ && _.mode) || 'development';
    const isProduction = mode === 'production';
    const sourcePath = __dirname + '/src/';
    const buildRootPath = __dirname + '/../build/front/';

    const jsLoaders = [{loader: "babel-loader", options: babelConfigJs}];

    let styleClasses = {}
    let stylesNext = 1
    let stylesAlpha1 = 'qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM_'.split('').sort(() => Math.random() * 2 - 1).join('')
    let stylesAlpha2 = 'qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM_-0123456789'.split('').sort(() => Math.random() * 2 - 1).join('')
    const cssLoader = [
        'style-loader',
        {
            loader: 'css-loader',
            options: {
                modules: {
                    getLocalIdent(loaderContext, localIdentName, localName, options) {
                        if (!isProduction) {
                            return localName
                        }
                        if (!styleClasses.hasOwnProperty(localName)) {
                            let next = stylesNext ++;
                            let nextStr = []
                            let arr = stylesAlpha1
                            while (next > 0) {
                                let ch = next % arr.length
                                next = Math.round((next - ch) / arr.length)
                                nextStr.push(arr.charAt(ch))
                                arr = stylesAlpha2
                            }
                            styleClasses[localName] = nextStr.join('')
                        }
                        return styleClasses[localName]
                    }
                }
            }
        }

    ];

    const scssLoaders = [
        ...cssLoader,
        {
            loader: "sass-loader",
            options: {
                additionalData: '@use "sass:map"; @use "sass:list"; @import "commonVariables.scss";',
                sassOptions: {
                    includePaths: [sourcePath + 'styles']
                }
            }
        }
    ];
    const rootComponents = [
        {
            entry: 'DotsGame',
            html: 'index.html',
            manifest: {
                filename: '[name]-'+VERSION+'.[ext]',
                fingerprints: false,
                publicPath: '',
                name: 'DotsGame',
                short_name: 'DotsGame',
                description: 'DotsGame',
                background_color: "#000000",
                theme_color: "#000000",
                display: "fullscreen",
                orientation: "portrait",
                start_url: './index.html',
                icons: [
                    {src: sourcePath + "i/favicon.png", size: '16x16'},
                    {src: sourcePath + "i/favicon144.png", size: '144x144'},
                ]
            }
        },
    ];

    return {
        mode: mode,
        entry: {
            "DotsGame": {
                import: sourcePath + "DotsGame/index.js"
            },
        },
        output: {
            path: buildRootPath,
            publicPath: '',
            filename: '[name]-'+VERSION+'.js',
            chunkFilename: '[name]-' + VERSION + '.js',
            pathinfo: !isProduction
        },
        module: {
            rules: [
                {
                    test: /\.js$/,
                    exclude: /node_modules/,
                    // exclude: file => (
                    //     /node_modules/.test(file) &&
                    //     !/\.vue\.js/.test(file)
                    // ),
                    use: jsLoaders
                },
                {
                    test: /\.vue$/,
                    use: [
                        {
                            loader: 'vue-loader',
                            options: {
                                productionMode: isProduction,
                                compilerOptions: {
                                    nodeTransforms: [vueFormsModuleTransform, vueVarModuleTransform]
                                },
                            }
                        },

                    ]
                },
                {
                    test: /\.s[ac]ss$/,
                    use: scssLoaders
                },
                {
                    test: /\.css$/,
                    use: cssLoader
                },
                {
                    test: /\.html$/,
                    loader: 'html-loader',
                    options: {
                        minifyJS: true
                    }
                },
                {
                    test: /\.(png|jpg|gif|ico|svg|eot|ttf|woff|woff2|ogg)$/,
                    loader: 'file-loader',
                    options: {
                        emitFile: true,
                        // outputPath: 'src/main/webapp/a/',
                        // publicPath: 'a/',
                        name: "[name]"+VERSION+".[ext]"
                    }
                }
            ]
        },
        resolve: {
            alias: {
                'i': sourcePath + 'i',
                's': sourcePath + 'styles',
                'api$': sourcePath + 'api.js',
            },
            extensions: ['*', '.js', '.vue', '.json'],
            symlinks: false
        },
        plugins: [
            new VueLoader.VueLoaderPlugin(),
            new webpack.DefinePlugin({
                _APP_TITLE_: JSON.stringify(TITLE),
                _VERSION_: JSON.stringify(VERSION),
                __VUE_OPTIONS_API__: true,
                __VUE_PROD_DEVTOOLS__: false,
            }),
            ...(() => {
                let roots = [];
                rootComponents.forEach(c => {
                    if (c.html) {
                        const manifest = c.manifest ? new WebpackPwaManifest(c.manifest) : null;
                        if (manifest) {
                            roots.push(manifest);
                        }
                        roots.push(
                            new HtmlWebpackPlugin({
                                chunks: ['common', ...(c.addChunks || []/* TODO Автоматизировать это! */), c.entry],
                                inject: false,
                                hash: false,
                                favicon: sourcePath + 'i/favicon.ico',
                                template: sourcePath + 'start/index.html.ejs',
                                templateParameters: function (compilation, assets, options) {
                                    let assetList = [], assetSize = 0;
                                    for (let asset of assets.css.concat(assets.js)) {
                                        assetList.push(asset);
                                        /* Тут надо как-то передвать реальный размер ассета. Сейчас он тут примерно равен*/
                                        assetSize += compilation.assets[asset].size();
                                    }
                                    return {
                                        compilation, assets, assetList, assetSize, options,
                                        title: c.title || TITLE,
                                        version: VERSION,
                                        width: null,
                                        manifest: manifest && manifest.manifest.url
                                    }
                                },
                                filename: buildRootPath + c.html,
                                minify: {
                                    minifyJS: true,
                                    collapseBooleanAttributes: true,
                                    collapseInlineTagWhitespace: true,
                                    collapseWhitespace: true,
                                    conservativeCollapse: false,
                                    decodeEntities: true,
                                    html5: true,
                                    includeAutoGeneratedTags: false,
                                    minifyCSS: true,
                                    removeAttributeQuotes: true,
                                    removeComments: true,
                                    removeOptionalTags: true,
                                    removeRedundantAttributes: true,
                                    removeScriptTypeAttributes: true,
                                    useShortDoctype: true
                                }

                            })
                        )
                    }
                });
                return roots;
            })()
        ],
        optimization: {
            splitChunks: {
                cacheGroups: {
                    vendor: {
                        test: /node_modules/,
                        chunks: 'initial',
                        name: 'common',
                        enforce: true
                    },
                }
            },
            usedExports: true
        }
    };
}


module.exports.default = function (env, _) {
    const mode = (_ && _.mode) || 'development';

    const configs = [
        DotsGameConfig.apply(this, arguments),
    ];

    if (mode === 'production') {
        const prodConfig = {
            stats: 'minimal'
        }
        const { merge } = require('webpack-merge');
        for (let i = 0; i < configs.length; i++){
            configs[i] = merge([configs[i], prodConfig]);
        }
    }
    if (mode === 'development') {
        const devConfig = {
            devtool: 'inline-source-map',
            devServer: {
                stats: 'errors-warnings',
                contentBase: staticRootPath,
                compress: true,
                port: (() => {
                    let r = /^.*:(\d+)$/.exec(devServerPublic);
                    if (!r) throw Error("devServerPublic должен содержать порт")
                    return parseInt(r[1])
                })(),
                public: devServerPublic,
                host: '0.0.0.0',
                proxy: {
                    '/a': devBackend,
                    '/up': devBackend,
                    '/a/s/': {
                        target: devBackend.replace(/^http/, 'ws'),
                        ws: true
                    }
                },
                hot: true
            }
        };
        const {merge} = require('webpack-merge');
        for (let i = 0; i < configs.length; i++){
            configs[i] = merge([configs[i], devConfig]);
        }
    }
    return configs
}