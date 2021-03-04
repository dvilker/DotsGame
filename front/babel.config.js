module.exports = {
    "presets": [
        [
            "@babel/preset-env",
            {
                "targets": [
                    // "> 0%",
                    "last 10 years",
                    // "not dead"
                ],
                "exclude": [
                    "transform-regenerator",
                    "transform-async-to-generator",
                    "transform-classes"
                ]
            }
        ]
    ],
    "plugins": []
}