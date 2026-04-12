const bcrypt = require('bcryptjs');
const match = bcrypt.compareSync('Admin@123', '$2a$10$kZWwjOQjP0jRXgB9MdJOQOIgAO.6CC6P.VKGw4R06/.z82RMTUPLW');
console.log(match);
