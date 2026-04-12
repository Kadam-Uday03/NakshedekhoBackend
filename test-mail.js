const nodemailer = require('nodemailer');

const transporter = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: 'nakshedekho@gmail.com',
    pass: 'typjgdaobadojbux'
  }
});

transporter.verify(function(error, success) {
  if (error) {
    console.log("Error for nakshedekho@gmail.com:", error.message);
  } else {
    console.log("Server is ready to take our messages for nakshedekho@gmail.com");
  }
});

const transporter2 = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: 'nakshedekho2026@gmail.com',
    pass: 'typjgdaobadojbux'
  }
});

transporter2.verify(function(error, success) {
  if (error) {
    console.log("Error for nakshedekho2026@gmail.com:", error.message);
  } else {
    console.log("Server is ready to take our messages for nakshedekho2026@gmail.com");
  }
});
